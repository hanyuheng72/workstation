package com.workstation.modules.task.dto;

import java.time.LocalDate;

public record CalendarDayVO(
        LocalDate date,
        int total,
        int done) {
}
