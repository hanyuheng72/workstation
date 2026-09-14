package com.workstation.modules.task.dto;

import com.workstation.modules.task.entity.RecurrenceType;
import com.workstation.modules.task.entity.TaskType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * planDate 留空表示今天。
 * 长期任务会忽略 planDate —— 它不生成每日实例，dueDate 才是它的截止日。
 */
public record TaskRequest(
        @NotBlank(message = "请填写任务内容")
        @Size(max = 100, message = "任务内容最多 100 字")
        String title,

        @Size(max = 500, message = "备注最多 500 字")
        String description,

        @NotNull(message = "请选择任务类型")
        TaskType taskType,

        @Min(value = 0, message = "优先级取值不合法")
        @Max(value = 9, message = "优先级取值不合法")
        Integer priority,

        LocalDate planDate,

        LocalDate dueDate,

        RecurrenceType recurrenceType,

        @Min(value = 1, message = "重复间隔至少为 1")
        @Max(value = 365, message = "重复间隔过大")
        Integer recurrenceInterval,

        LocalDate recurrenceEndDate) {
}
