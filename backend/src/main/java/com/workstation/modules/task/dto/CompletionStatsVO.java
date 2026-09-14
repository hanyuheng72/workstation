package com.workstation.modules.task.dto;

import java.time.LocalDate;

public record CompletionStatsVO(
        LocalDate start,
        LocalDate end,
        int total,
        int done,
        int completionRate) {
}
