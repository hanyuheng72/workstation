package com.workstation.modules.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workstation.common.exception.BusinessException;
import com.workstation.common.result.ErrorCode;
import com.workstation.modules.ai.dto.AiSummaryVO;
import com.workstation.modules.ai.entity.AiSummary;
import com.workstation.modules.ai.mapper.AiSummaryMapper;
import com.workstation.modules.dashboard.DashboardService;
import com.workstation.modules.dashboard.dto.DashboardOverviewVO;
import com.workstation.modules.finance.FinanceStatsService;
import com.workstation.modules.finance.dto.CategoryStatVO;
import com.workstation.modules.finance.entity.TransactionType;
import com.workstation.modules.workout.WorkoutStatsService;
import com.workstation.modules.workout.dto.PersonalBestVO;
import com.workstation.modules.workout.dto.WorkoutFrequencyVO;
import com.workstation.modules.workout.dto.WorkoutVolumeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 每日总结与专项分析。
 *
 * 总结会缓存：仪表盘每次加载都读缓存，只有用户点「重新生成」才重新调用模型。
 * 这既省钱，也让首页打开速度不受模型响应时间影响。
 */
@Service
public class AiSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AiSummaryService.class);

    private final AiSummaryMapper summaryMapper;
    private final DashboardService dashboardService;
    private final FinanceStatsService financeStatsService;
    private final WorkoutStatsService workoutStatsService;
    private final AiMemoryService memoryService;
    private final DeepSeekClient client;
    private final ObjectMapper objectMapper;

    public AiSummaryService(AiSummaryMapper summaryMapper, DashboardService dashboardService,
                            FinanceStatsService financeStatsService,
                            WorkoutStatsService workoutStatsService,
                            AiMemoryService memoryService,
                            DeepSeekClient client, ObjectMapper objectMapper) {
        this.summaryMapper = summaryMapper;
        this.dashboardService = dashboardService;
        this.financeStatsService = financeStatsService;
        this.workoutStatsService = workoutStatsService;
        this.memoryService = memoryService;
        this.client = client;
        this.objectMapper = objectMapper;
    }

    /**
     * 只读缓存。**不会**触发模型调用——否则仪表盘每次打开首页都会花一次钱，
     * 页面还要等模型几秒钟。没有缓存时返回 content 为 null，由前端显示「生成」按钮。
     */
    public AiSummaryVO today() {
        LocalDate today = LocalDate.now();
        AiSummary cached = findByDate(today);
        if (cached == null) {
            return new AiSummaryVO(today, null, null, true);
        }
        return new AiSummaryVO(cached.getSummaryDate(), cached.getContent(),
                cached.getGeneratedAt(), true);
    }

    /** 生成或刷新今日总结。只有用户明确点了「生成 / 重新生成」才会走到这里 */
    @Transactional
    public AiSummaryVO generateToday() {
        requireConfigured();
        LocalDate today = LocalDate.now();
        DashboardOverviewVO overview = dashboardService.overview();

        // 数据全为空时不值得花一次调用，直接给一句实话
        if (isEmptyDay(overview)) {
            String content = "今天还没有任何记录。记一条体重、一次训练或一笔开销，这里就会有内容了。";
            return save(today, content, toJson(overview), 0, 0);
        }

        DeepSeekClient.Completion completion = client.complete(List.of(
                ChatMessage.system(SUMMARY_PROMPT),
                ChatMessage.user(withMemories(toJson(overview)))), false);

        return save(today, completion.content().trim(), toJson(overview),
                completion.promptTokens(), completion.completionTokens());
    }

    /**
     * 把长期画像拼在数据前面。
     * 有了它，总结才能说出「按你的增肌目标，今天蛋白质摄入可能不够」这种话，
     * 而不是干巴巴地复述数字。
     */
    private String withMemories(String data) {
        String memories = memoryService.describeForPrompt();
        if (memories.isBlank()) {
            return "## 今天的数据\n" + data;
        }
        return "## 关于用户的长期记忆\n" + memories + "\n## 今天的数据\n" + data;
    }

    /** 本月消费分析 */
    public String analyzeExpense(String month) {
        requireConfigured();
        var target = FinanceStatsService.parseMonth(month);
        List<CategoryStatVO> categories = financeStatsService.categoryStats(TransactionType.EXPENSE, target);
        var daily = financeStatsService.dailyTrend(target);

        if (categories.isEmpty() && daily.stream().allMatch(p -> p.expense().signum() == 0)) {
            throw BusinessException.badRequest("这个月还没有支出记录，没什么可分析的");
        }

        DeepSeekClient.Completion completion = client.complete(List.of(
                ChatMessage.system(EXPENSE_PROMPT),
                ChatMessage.user(withMemories(toJson(new ExpenseContext(categories, daily))))), false);
        return completion.content().trim();
    }

    /** 近期训练分析 */
    public String analyzeWorkout() {
        requireConfigured();
        WorkoutFrequencyVO frequency = workoutStatsService.frequency("week");
        WorkoutVolumeVO volume = workoutStatsService.volume(null, null);
        List<PersonalBestVO> prs = workoutStatsService.personalBests();

        if (frequency.totalSessions() == 0 && prs.isEmpty()) {
            throw BusinessException.badRequest("还没有训练记录，没什么可分析的");
        }

        DeepSeekClient.Completion completion = client.complete(List.of(
                ChatMessage.system(WORKOUT_PROMPT),
                ChatMessage.user(withMemories(toJson(new WorkoutContext(frequency, volume, prs))))), false);
        return completion.content().trim();
    }

    public boolean isConfigured() {
        return client.isConfigured();
    }

    public String model() {
        return client.model();
    }

    // ---------------- 内部 ----------------

    private AiSummary findByDate(LocalDate date) {
        return summaryMapper.selectOne(
                Wrappers.lambdaQuery(AiSummary.class).eq(AiSummary::getSummaryDate, date));
    }

    private AiSummaryVO save(LocalDate date, String content, String snapshot, int promptTokens, int completionTokens) {
        AiSummary record = findByDate(date);
        if (record == null) {
            record = new AiSummary();
            record.setSummaryDate(date);
        }
        record.setContent(content);
        record.setModel(client.model());
        record.setDataSnapshot(snapshot);
        record.setPromptTokens(promptTokens);
        record.setCompletionTokens(completionTokens);
        record.setGeneratedAt(LocalDateTime.now());

        if (record.getId() == null) {
            summaryMapper.insert(record);
        } else {
            summaryMapper.updateById(record);
        }
        return new AiSummaryVO(date, content, record.getGeneratedAt(), false);
    }

    private void requireConfigured() {
        if (!client.isConfigured()) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        }
    }

    private static boolean isEmptyDay(DashboardOverviewVO overview) {
        return overview.tasks().total() == 0
                && overview.weight().weightKg() == null
                && overview.workout().exerciseCount() == 0
                && overview.finance().todayExpense().signum() == 0;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("序列化快照失败: {}", ex.getMessage());
            return "{}";
        }
    }

    private record ExpenseContext(List<CategoryStatVO> categoriesByAmount, List<?> dailyTrend) {
    }

    private record WorkoutContext(WorkoutFrequencyVO frequency, WorkoutVolumeVO volume,
                                  List<PersonalBestVO> personalBests) {
    }

    private static final String SUMMARY_PROMPT = """
            你是「个人工作台」里的数据助手。用户会给你一段文字，里面可能有两部分：
            「关于用户的长期记忆」和「今天的数据」。

            请写一段中文总结，要求：
            1. 3 到 5 句，简洁自然，口语化，不要用 markdown 标题或列表符号。
            2. 只谈数据里**确实有**的方面；某个方面没有数据就完全不提，不要编造，也不要为了凑字数硬提。
            3. 指出一个值得注意的点，例如任务没做完、今天支出明显偏高、训练量在往上走。
            4. 最后给一条具体、可执行的建议。**如果给了长期记忆，建议要结合它**——
               例如记忆里写了增肌目标，就不要泛泛说「注意饮食」，而要说跟目标相关的事。
            5. 不要罗列记忆本身，也不要提「根据你的记忆」这类话，把它当成你本来就知道的事。
            6. 不要把所有数字都复述一遍，挑重点说。
            7. 直接输出总结正文，不要任何开场白。
            """;

    private static final String EXPENSE_PROMPT = """
            你是「个人工作台」里的消费分析助手。用户会给你一份本月支出的 JSON 数据，
            包含按分类汇总的金额与占比，以及逐日的收支。

            请用中文写一段分析，要求：
            1. 3 到 5 句，指出花得最多的是哪一类、占比多少。
            2. 从数据里找出一个具体现象，例如某几天的开销明显高于其余日子、某个分类占比异常。
            3. 给一条可执行的省钱建议，要具体到某类开销，不要说「少花钱」这种空话。
            4. 只依据给到的数据，不要推测用户没提供的信息。
            5. 直接输出分析正文，不要开场白，不要 markdown 标题。
            """;

    private static final String WORKOUT_PROMPT = """
            你是「个人工作台」里的训练分析助手。用户会给你一份训练数据的 JSON，
            包含每周训练次数、各部位训练次数、训练总量与各动作的个人最佳重量。

            请用中文写一段分析，要求：
            1. 3 到 5 句，先概括最近的训练频率是否规律。
            2. 指出各部位之间是否明显不均衡（例如某个部位练得特别少或完全没练到）。
            3. 结合个人最佳重量，指出哪个动作有进步空间。
            4. 给一条具体建议，例如补上哪个部位、或某个动作可以尝试多少重量。
            5. 只依据给到的数据，不要编造用户没做过的动作。
            6. 直接输出分析正文，不要开场白，不要 markdown 标题。
            """;
}
