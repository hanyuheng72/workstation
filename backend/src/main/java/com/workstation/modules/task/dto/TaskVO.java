package com.workstation.modules.task.dto;

import com.workstation.modules.task.entity.RecurrenceType;
import com.workstation.modules.task.entity.TaskStatus;
import com.workstation.modules.task.entity.TaskType;

import java.time.LocalDate;

public record TaskVO(
        Long id,
        String title,
        String description,
        TaskType taskType,
        Integer priority,
        LocalDate planDate,
        LocalDate dueDate,
        RecurrenceType recurrenceType,
        Integer recurrenceInterval,
        LocalDate recurrenceEndDate,
        TaskStatus status,
        boolean recurring,
        /** 在日历上落位的日期；长期任务为 null，因为它不占日历 */
        LocalDate anchorDate) {
}
