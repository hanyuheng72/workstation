package com.workstation.modules.task.dto;

import com.workstation.modules.task.entity.RecurrenceType;
import com.workstation.modules.task.entity.TaskStatus;
import com.workstation.modules.task.entity.TaskType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 某一天上的一条任务。完成状态取自 task_occurrence，而不是 task 本身。 */
public record TaskOccurrenceVO(
        Long occurrenceId,
        Long taskId,
        String title,
        String description,
        TaskType taskType,
        RecurrenceType recurrenceType,
        Integer priority,
        LocalDate occurDate,
        TaskStatus status,
        LocalDateTime completedAt,
        boolean recurring) {
}
