package com.workstation.modules.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.workstation.common.exception.BusinessException;
import com.workstation.modules.ai.dto.AiMemoryVO;
import com.workstation.modules.finance.FinanceService;
import com.workstation.modules.finance.dto.CategoryVO;
import com.workstation.modules.finance.dto.TransactionRequest;
import com.workstation.modules.finance.dto.TransactionVO;
import com.workstation.modules.finance.entity.TransactionType;
import com.workstation.modules.task.TaskService;
import com.workstation.modules.task.dto.TaskRequest;
import com.workstation.modules.task.dto.TaskVO;
import com.workstation.modules.task.entity.RecurrenceType;
import com.workstation.modules.task.entity.TaskType;
import com.workstation.modules.weight.WeightService;
import com.workstation.modules.weight.dto.WeightUpsertRequest;
import com.workstation.modules.weight.dto.WeightVO;
import com.workstation.modules.workout.WorkoutService;
import com.workstation.modules.workout.dto.ExerciseVO;
import com.workstation.modules.workout.dto.WorkoutRequest;
import com.workstation.modules.workout.dto.WorkoutSetRequest;
import com.workstation.modules.workout.dto.WorkoutVO;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 把已确认的 AI 草稿写进业务表。
 *
 * 这是 AI 链路上**唯一**会改业务数据的地方，而且只会在用户点了确认之后被调用。
 * 它复用各模块已有的 Service，因此 AI 写进去的数据与手工录入的走完全相同的校验规则，
 * 模型绕不过任何一条业务约束。
 */
@Component
public class ActionExecutor {

    private final WeightService weightService;
    private final WorkoutService workoutService;
    private final FinanceService financeService;
    private final TaskService taskService;
    private final AiMemoryService memoryService;

    public ActionExecutor(WeightService weightService, WorkoutService workoutService,
                          FinanceService financeService, TaskService taskService,
                          AiMemoryService memoryService) {
        this.weightService = weightService;
        this.workoutService = workoutService;
        this.financeService = financeService;
        this.taskService = taskService;
        this.memoryService = memoryService;
    }

    /**
     * 生成给用户看的草稿预览。**只读**，不写任何业务表。
     * 模型可能给出字典里没有的动作名，这里如实标出来，
     * 免得用户确认之后才发现记不进去。
     */
    public String preview(String intent, JsonNode payload) {
        return switch (intent) {
            case IntentParser.INTENT_WEIGHT -> {
                BigDecimal weight = decimal(payload, "weightKg");
                yield weight == null ? "没能识别出体重数值" : "体重 " + plain(weight) + " kg";
            }
            case IntentParser.INTENT_WORKOUT -> {
                String name = text(payload, "exerciseName");
                JsonNode sets = payload.path("sets");
                String setText = sets.isArray() && !sets.isEmpty()
                        ? sets.size() + " 组" : "没有识别出组数";
                String known = (name != null && workoutService.findExerciseByName(name) != null)
                        ? "" : "（动作不在字典里，确认时会提示先添加）";
                yield "训练 " + (name == null ? "未知动作" : name) + " · " + setText + known;
            }
            case IntentParser.INTENT_EXPENSE, IntentParser.INTENT_INCOME -> {
                BigDecimal amount = decimal(payload, "amount");
                String code = text(payload, "categoryCode");
                CategoryVO category = financeService.findCategoryByCode(code,
                        intent.equals(IntentParser.INTENT_EXPENSE)
                                ? TransactionType.EXPENSE : TransactionType.INCOME);
                String label = intent.equals(IntentParser.INTENT_EXPENSE) ? "支出" : "收入";
                yield amount == null
                        ? "没能识别出金额"
                        : label + " ¥" + plain(amount) + " · " + category.name();
            }
            case IntentParser.INTENT_TASK -> {
                String title = text(payload, "title");
                String recurrence = text(payload, "recurrenceType");
                String suffix = recurrence == null || "NONE".equalsIgnoreCase(recurrence)
                        ? "" : "（重复）";
                yield "任务「" + (title == null ? "未命名" : title) + "」" + suffix;
            }
            case IntentParser.INTENT_MEMORY -> "记住：" + text(payload, "content");
            default -> "无法预览";
        };
    }

    @Transactional
    public ExecutedAction execute(String intent, JsonNode payload) {
        return switch (intent) {
            case IntentParser.INTENT_WEIGHT -> recordWeight(payload);
            case IntentParser.INTENT_WORKOUT -> recordWorkout(payload);
            case IntentParser.INTENT_EXPENSE -> recordTransaction(payload, TransactionType.EXPENSE);
            case IntentParser.INTENT_INCOME -> recordTransaction(payload, TransactionType.INCOME);
            case IntentParser.INTENT_TASK -> createTask(payload);
            case IntentParser.INTENT_MEMORY -> remember(payload);
            default -> throw BusinessException.badRequest("不认识这个动作：" + intent);
        };
    }

    /**
     * 记下一条关于用户的长期信息。
     * 与「记数据」共用同一套确认链路——模型只能提议，用户点过才会写进表里。
     */
    private ExecutedAction remember(JsonNode payload) {
        String content = text(payload, "content");
        if (content == null || content.isBlank()) {
            throw BusinessException.badRequest("这条记忆没有内容");
        }
        AiMemoryVO saved = memoryService.add(content, text(payload, "category"), "CHAT");
        return new ExecutedAction("MEMORY", saved.id(), "已记住：" + saved.content());
    }

    private ExecutedAction recordWeight(JsonNode payload) {
        BigDecimal weight = decimal(payload, "weightKg");
        if (weight == null) {
            throw BusinessException.badRequest("这句里没有识别出体重数值");
        }
        WeightVO saved = weightService.upsert(new WeightUpsertRequest(
                date(payload, "date"), weight, text(payload, "note")));
        return new ExecutedAction("WEIGHT", saved.id(), "已记录体重 " + plain(saved.weightKg()) + "kg");
    }

    private ExecutedAction recordWorkout(JsonNode payload) {
        String name = text(payload, "exerciseName");
        if (name == null || name.isBlank()) {
            throw BusinessException.badRequest("这句里没有识别出训练动作");
        }
        ExerciseVO exercise = workoutService.findExerciseByName(name);
        if (exercise == null) {
            throw BusinessException.badRequest(
                    "动作「" + name + "」不在动作字典里，请先到健身页添加这个动作");
        }

        JsonNode sets = payload.path("sets");
        if (!sets.isArray() || sets.isEmpty()) {
            throw BusinessException.badRequest("这句里没有识别出组数");
        }
        List<WorkoutSetRequest> requests = new ArrayList<>();
        for (JsonNode set : sets) {
            requests.add(new WorkoutSetRequest(
                    decimal(set, "weightKg"),
                    integer(set, "reps"),
                    decimal(set, "durationMin"),
                    decimal(set, "distanceKm")));
        }

        WorkoutVO saved = workoutService.create(new WorkoutRequest(
                date(payload, "date"), exercise.id(), requests, text(payload, "note")));
        return new ExecutedAction("WORKOUT", saved.id(),
                "已记录 " + exercise.name() + " " + describeSets(saved));
    }

    private ExecutedAction recordTransaction(JsonNode payload, TransactionType type) {
        BigDecimal amount = decimal(payload, "amount");
        if (amount == null || amount.signum() <= 0) {
            throw BusinessException.badRequest("这句里没有识别出有效金额");
        }
        CategoryVO category = financeService.findCategoryByCode(text(payload, "categoryCode"), type);

        // AI 不传时间，一律用「现在」；发生日期允许它从「昨天」这类说法推算
        TransactionVO saved = financeService.create(new TransactionRequest(
                type, amount, category.id(), date(payload, "date"), null, text(payload, "note")));

        String label = type == TransactionType.EXPENSE ? "支出" : "收入";
        return new ExecutedAction("TRANSACTION", saved.id(),
                "已记录" + label + " ¥" + plain(saved.amount()) + "（" + category.name() + "）");
    }

    private ExecutedAction createTask(JsonNode payload) {
        String title = text(payload, "title");
        if (title == null || title.isBlank()) {
            throw BusinessException.badRequest("这句里没有识别出任务内容");
        }
        LocalDate date = date(payload, "date");
        RecurrenceType recurrence = recurrence(text(payload, "recurrenceType"));

        TaskVO saved = taskService.create(new TaskRequest(
                title, null, TaskType.TODAY, 0,
                date, null,
                recurrence, recurrence == RecurrenceType.NONE ? null : 1, null));

        String suffix = recurrence == RecurrenceType.NONE ? "" : "（每" + (recurrence == RecurrenceType.DAILY ? "天" : "周") + "）";
        return new ExecutedAction("TASK", saved.id(), "已添加任务「" + title + "」" + suffix);
    }

    // ---------------- 取值助手：模型输出的类型不一定规整，一律容错读取 ----------------

    private static LocalDate date(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank() || "null".equals(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ex) {
            throw BusinessException.badRequest("日期格式不对：" + value);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText(null);
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.decimalValue();
        }
        String raw = value.asText("").replaceAll("[^0-9.\\-]", "");
        return raw.isEmpty() ? null : new BigDecimal(raw);
    }

    private static Integer integer(JsonNode node, String field) {
        BigDecimal value = decimal(node, field);
        return value == null ? null : value.intValue();
    }

    private static RecurrenceType recurrence(String raw) {
        if (raw == null) {
            return RecurrenceType.NONE;
        }
        try {
            return RecurrenceType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return RecurrenceType.NONE;
        }
    }

    private static String plain(BigDecimal value) {
        return value == null ? "—" : value.stripTrailingZeros().toPlainString();
    }

    private static String describeSets(WorkoutVO workout) {
        if (workout.cardio()) {
            return workout.sets().stream()
                    .map(set -> plain(set.durationMin()) + "分钟")
                    .reduce((a, b) -> a + "、" + b).orElse("");
        }
        return workout.sets().size() + " 组 × "
                + workout.sets().stream().map(set -> String.valueOf(set.reps())).findFirst().orElse("?")
                + " 次";
    }
}
