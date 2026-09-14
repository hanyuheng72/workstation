package com.workstation.modules.task.dto;

import java.time.LocalDate;
import java.util.List;

public record TodayTasksVO(
        LocalDate date,
        List<TaskOccurrenceVO> items,
        int total,
        int done,
        /** 完成率，0-100 的整数；没有任务时为 0 */
        int completionRate) {
}
