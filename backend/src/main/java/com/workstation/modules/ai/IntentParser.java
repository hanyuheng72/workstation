package com.workstation.modules.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workstation.common.exception.BusinessException;
import com.workstation.common.result.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 把用户的一句话解析成「回应 + 可选的动作草稿」。
 *
 * 这里只负责解析，不碰任何业务表——真正落库在 ActionExecutor，
 * 而且必须等用户在界面上确认之后才会被调用。
 */
@Component
public class IntentParser {

    private static final Logger log = LoggerFactory.getLogger(IntentParser.class);

    public static final String INTENT_WEIGHT = "LOG_WEIGHT";
    public static final String INTENT_WORKOUT = "LOG_WORKOUT";
    public static final String INTENT_EXPENSE = "LOG_EXPENSE";
    public static final String INTENT_INCOME = "LOG_INCOME";
    public static final String INTENT_TASK = "CREATE_TASK";

    private final DeepSeekClient client;
    private final ObjectMapper objectMapper;

    public IntentParser(DeepSeekClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public Parsed parse(String userText) {
        DeepSeekClient.Completion completion = client.complete(
                List.of(ChatMessage.system(systemPrompt()), ChatMessage.user(userText)), true);

        JsonNode root = readJson(completion.content());
        String reply = root.path("reply").asText("").trim();
        if (reply.isEmpty()) {
            reply = "收到。";
        }

        JsonNode action = root.path("action");
        if (action.isMissingNode() || action.isNull() || !action.isObject()) {
            return new Parsed(reply, null, null, completion.promptTokens(), completion.completionTokens());
        }

        String intent = action.path("intent").asText("");
        JsonNode payload = action.path("payload");
        if (intent.isBlank() || !payload.isObject()) {
            return new Parsed(reply, null, null, completion.promptTokens(), completion.completionTokens());
        }

        return new Parsed(reply, intent, payload,
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

    private String systemPrompt() {
        LocalDate today = LocalDate.now();
        return """
                你是「个人工作台」App 里的助手，帮用户用自然语言记录日常数据。

                今天是 %s（星期%s）。用户说「昨天」「前天」时按这个日期推算。

                你能识别的动作有 5 种，payload 结构必须严格遵守：

                1. LOG_WEIGHT 记录体重
                   {"weightKg": 数字, "date": "YYYY-MM-DD" 或 null, "note": 字符串或 null}
                   例：「我今天体重65.3公斤」→ {"weightKg": 65.3}

                2. LOG_WORKOUT 记录训练
                   {"exerciseName": "动作名", "date": "YYYY-MM-DD" 或 null, "note": 字符串或 null,
                    "sets": [{"weightKg": 数字或null, "reps": 整数或null,
                              "durationMin": 数字或null, "distanceKm": 数字或null}]}
                   例：「今天做了卧推，60公斤，4组，每组10次」
                   → {"exerciseName": "卧推", "sets": [{"weightKg":60,"reps":10},
                                                      {"weightKg":60,"reps":10},
                                                      {"weightKg":60,"reps":10},
                                                      {"weightKg":60,"reps":10}]}
                   有氧用 durationMin / distanceKm。例：「跑了30分钟5公里」
                   → {"exerciseName": "跑步", "sets": [{"durationMin":30,"distanceKm":5}]}

                3. LOG_EXPENSE 记录支出
                   {"amount": 数字, "date": "YYYY-MM-DD" 或 null, "note": 字符串或 null,
                    "categoryCode": "FOOD|TRANSPORT|SHOPPING|GAME|STUDY|HOUSING|OTHER_EXPENSE"}
                   例：「今天吃饭花了35元」→ {"amount": 35, "categoryCode": "FOOD"}

                4. LOG_INCOME 记录收入
                   {"amount": 数字, "date": "YYYY-MM-DD" 或 null, "note": 字符串或 null,
                    "categoryCode": "ALLOWANCE|SALARY|OTHER_INCOME"}

                5. CREATE_TASK 创建任务
                   {"title": "任务内容", "date": "YYYY-MM-DD" 或 null,
                    "recurrenceType": "NONE|DAILY|WEEKLY"}

                输出要求：只输出一个 JSON 对象，不要解释文字，不要代码块标记。格式固定为：
                {"reply": "给用户的一句中文回应，简短自然",
                 "action": null 或 {"intent": "上面五种之一", "payload": {...}}}

                关于 reply 的措辞（很重要）：
                - 当 action 不为 null 时，数据**还没有**被记录，要等用户在界面上点确认。
                  所以绝对不要说「已记录」「已经记好」这类完成态的话，
                  而应该说成「体重 65.3kg，确认后我就记上」这样等待确认的语气。
                - 当 action 为 null 时，就是普通对话，正常回答即可。

                判断规则：
                - 只有用户明确表达了要记录的信息才输出 action；闲聊、提问、让你分析数据时 action 必须为 null。
                - 金额、重量、次数一律用纯数字，不要带单位。
                - 用户说「今天」或完全没提时间时，date 填 null，由后端补今天。
                - 分类拿不准就用 OTHER_EXPENSE / OTHER_INCOME，并在 reply 里说明。
                - 绝对不要编造用户没说过的数据。
                """.formatted(today, chineseWeekday(today));
    }

    private static String chineseWeekday(LocalDate date) {
        return switch (date.getDayOfWeek()) {
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
