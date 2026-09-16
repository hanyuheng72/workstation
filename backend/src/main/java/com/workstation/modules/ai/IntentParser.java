package com.workstation.modules.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workstation.common.exception.BusinessException;
import com.workstation.common.result.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 把用户的一句话解析成「回应 + 可选的动作草稿 + 可选的记忆草稿」。
 *
 * 这里只负责解析，不碰任何业务表——真正落库在 ActionExecutor，
 * 而且必须等用户在界面上确认之后才会被调用。记忆也一样走这条链路。
 */
@Component
public class IntentParser {

    private static final Logger log = LoggerFactory.getLogger(IntentParser.class);

    public static final String INTENT_WEIGHT = "LOG_WEIGHT";
    public static final String INTENT_WORKOUT = "LOG_WORKOUT";
    public static final String INTENT_EXPENSE = "LOG_EXPENSE";
    public static final String INTENT_INCOME = "LOG_INCOME";
    public static final String INTENT_TASK = "CREATE_TASK";
    /** 记住一条关于用户的长期信息，与上面几种「记录数据」区分开 */
    public static final String INTENT_MEMORY = "SAVE_MEMORY";

    private final DeepSeekClient client;
    private final ObjectMapper objectMapper;

    public IntentParser(DeepSeekClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    /**
     * @param history  最近几条对话，让助手能接住「那再记一笔」这类上下文
     * @param memories 用户的长期画像，空串表示还没有
     * @param snapshot 今天的数据快照，让助手能回答「我这个月花了多少」这类问题
     */
    public Parsed parse(String userText, List<ChatMessage> history, String memories, String snapshot) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(systemPrompt(memories, snapshot)));
        messages.addAll(history);
        messages.add(ChatMessage.user(userText));

        // 模型偶发会退化成「只吐一片空白」——结构合法、finish_reason 是 stop，
        // 但正文全是空格。temperature 0.1 下同样请求会得到同样结果，所以重试必须换温度。
        Parsed first = attempt(messages, 0.1);
        if (first != null) {
            return first;
        }
        log.warn("模型返回空正文，换温度重试");
        Parsed retry = attempt(messages, 0.9);
        if (retry != null) {
            return retry;
        }
        // 再不行就丢掉历史，用最短的上下文再试一次
        log.warn("换温度后仍为空，去掉历史再试");
        List<ChatMessage> minimal = new ArrayList<>();
        minimal.add(messages.get(0));
        minimal.add(messages.get(messages.size() - 1));
        Parsed last = attempt(minimal, 0.9);
        if (last != null) {
            return last;
        }
        log.warn("三次都返回空正文，退回兜底文案");
        return new Parsed("抱歉，我没组织好回答，能再说一遍吗？", null, null, 0, 0);
    }

    /** 返回 null 表示这次输出完全为空（全是空白），需要换条件重试 */
    private Parsed attempt(List<ChatMessage> messages, double temperature) {
        DeepSeekClient.Completion completion = client.complete(messages, true, temperature);

        JsonNode root = readJson(completion.content());
        String reply = root.path("reply").asText("").trim();

        JsonNode action = root.path("action");
        JsonNode memory = root.path("memory");

        String intent = null;
        JsonNode payload = null;

        if (action.isObject() && !action.isNull()) {
            String actionIntent = action.path("intent").asText("");
            JsonNode actionPayload = action.path("payload");
            if (!actionIntent.isBlank() && actionPayload.isObject()) {
                intent = actionIntent;
                payload = actionPayload;
            }
        }

        // 没解析出动作时才看记忆：一条消息只推一个待确认项，避免界面上同时弹两张卡。
        // 记数据比记画像是更紧急的事，所以动作优先。
        if (intent == null && memory.isObject() && !memory.isNull()) {
            String content = memory.path("content").asText("").trim();
            if (!content.isEmpty()) {
                intent = INTENT_MEMORY;
                var node = objectMapper.createObjectNode();
                node.put("content", content);
                node.put("category", memory.path("category").asText("OTHER"));
                payload = node;
            }
        }

        if (reply.isEmpty() && intent == null) {
            return null;
        }

        return new Parsed(reply.isEmpty() ? "收到了。" : reply, intent, payload,
                completion.promptTokens(), completion.completionTokens());
    }

    /** 模型偶尔会裹一层 ```json 代码块，或者前后带解释文字，这里做一次容错抽取 */
    private JsonNode readJson(String content) {
        String text = content.trim();
        if (text.startsWith("```")) {
            int start = text.indexOf('\n');
            int end = text.lastIndexOf("```");
            if (start > 0 && end > start) {
                text = text.substring(start + 1, end).trim();
            }
        }
        if (!text.startsWith("{")) {
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start >= 0 && end > start) {
                text = text.substring(start, end + 1);
            }
        }

        try {
            return objectMapper.readTree(text);
        } catch (Exception ex) {
            log.warn("模型输出不是合法 JSON，无法解析意图");
            throw new BusinessException(ErrorCode.AI_PARSE_FAILED);
        }
    }

    private String systemPrompt(String memories, String snapshot) {
        LocalDate today = LocalDate.now();

        String memoryBlock = memories.isBlank()
                ? "（还没有关于用户的长期记忆）"
                : memories;

        return """
                你是「个人工作台」App 里的助手。用户用它记录任务、体重、健身和收支，
                你的职责有两件：帮他快速记录，以及结合他的真实情况给出有用的回应。

                今天是 %s（星期%s）。用户说「昨天」「前天」时按这个日期推算。

                ## 关于用户的长期记忆
                %s

                ## 用户今天的数据快照
                %s

                ## 你要输出的 JSON
                只输出一个 JSON 对象，不要解释文字，不要代码块标记：
                {"reply": "给用户的中文回应",
                 "action": null 或 {"intent": "...", "payload": {...}},
                 "memory": null 或 {"content": "...", "category": "..."}}

                ### action：把用户说的话记成数据
                五种，payload 结构必须严格遵守：

                1. LOG_WEIGHT 记录体重
                   {"weightKg": 数字, "date": "YYYY-MM-DD" 或 null, "note": 字符串或 null}
                   例：「我今天体重65.3公斤」→ {"weightKg": 65.3}

                2. LOG_WORKOUT 记录训练
                   {"exerciseName": "动作名", "date": "YYYY-MM-DD" 或 null, "note": 字符串或 null,
                    "sets": [{"weightKg": 数字或null, "reps": 整数或null,
                              "durationMin": 数字或null, "distanceKm": 数字或null}]}
                   例：「今天做了卧推，60公斤，4组，每组10次」
                   → {"exerciseName": "卧推", "sets": [{"weightKg":60,"reps":10} × 4 组]}
                   有氧用 durationMin / distanceKm。例：「跑了30分钟5公里」
                   → {"exerciseName": "跑步", "sets": [{"durationMin":30,"distanceKm":5}]}

                3. LOG_EXPENSE 记录支出
                   {"amount": 数字, "date": 或 null, "note": 或 null,
                    "categoryCode": "FOOD|TRANSPORT|SHOPPING|GAME|STUDY|HOUSING|OTHER_EXPENSE"}

                4. LOG_INCOME 记录收入
                   {"amount": 数字, "date": 或 null, "note": 或 null,
                    "categoryCode": "ALLOWANCE|SALARY|OTHER_INCOME"}

                5. CREATE_TASK 创建任务
                   {"title": "任务内容", "date": 或 null, "recurrenceType": "NONE|DAILY|WEEKLY"}

                ### memory：记住关于用户的长期信息
                当用户透露了关于他自己的、以后一直有用的事实时，提议记下来。
                例如身高体重、健身目标、饮食偏好、作息习惯、常去的健身房、正在攒钱买什么。

                判断标准（严格）：
                - 必须是**长期有效**的。今天吃了什么、今天心情如何，都不是记忆。
                - 必须是**记忆里还没有的**。上面「关于用户的长期记忆」已经写过的事，不要再提。
                - 一次只提一条，挑最有价值的。没有就填 null，**绝大多数时候都应该是 null**。

                content 写成一句独立的第三人称陈述，别用「用户说」开头：
                  好：「身高 173cm，正在增肌，目标体重 70kg」
                  好：「健身习惯是每周三练，偏好上午去」
                  差：「用户刚才说他身高173」← 这是转述，不是事实

                category 只能填 PROFILE（基本资料）/ GOAL（目标）/ PREFERENCE（偏好）/ HABIT（习惯）/ OTHER。

                ### reply 的措辞（很重要）
                - **reply 永远不能为空。** 哪怕你不知道该说什么，也要基于上面的数据给一句有用的回应。
                - action 不为 null 时，数据**还没有**被记录，要等用户点确认。
                  所以绝对不要说「已记录」「已经记好」，而要说「体重 65.3kg，确认后我就记上」。
                - memory 不为 null 时可以自然地问一句要不要记住。
                - 都没有时就是普通对话，正常回答即可。
                - 结合上面的记忆和快照来回应，别只复述数字。用户在问「我这个月花了多少」这类问题时，
                  直接根据快照回答，不要推脱说你看不到数据。

                判断规则：
                - 闲聊、提问、让你分析时 action 必须为 null。
                - 金额、重量、次数一律用纯数字，不要带单位。
                - 用户说「今天」或没提时间时，date 填 null，由后端补今天。
                - 分类拿不准就用 OTHER_EXPENSE / OTHER_INCOME，并在 reply 里说明。
                - 绝对不要编造用户没说过的数据。
                """.formatted(today, chineseWeekday(today), memoryBlock, snapshot);
    }

    private static String chineseWeekday(LocalDate date) {        return switch (date.getDayOfWeek()) {
            case MONDAY -> "一";
            case TUESDAY -> "二";
            case WEDNESDAY -> "三";
            case THURSDAY -> "四";
            case FRIDAY -> "五";
            case SATURDAY -> "六";
            case SUNDAY -> "日";
        };
    }
}
