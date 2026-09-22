package com.workstation.modules.focus.dto;

import java.time.LocalDate;

/** 某一天的专注时长与成败次数 */
public record DailyFocusVO(
        LocalDate date,
        int minutes,
        int successCount,
        int failCount) {
}
