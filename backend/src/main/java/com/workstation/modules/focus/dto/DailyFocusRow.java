package com.workstation.modules.focus.dto;

import java.time.LocalDate;

/**
 * 每日专注的聚合行，日趋势用。
 * 不是接口返回值，接口返回的是 DailyFocusVO。
 */
public record DailyFocusRow(
        LocalDate sessionDate,
        Integer successSeconds,
        Integer successCount,
        Integer failCount) {

    /** 区间里没有记录的那一天，补一行 0 */
    public static DailyFocusRow empty(LocalDate date) {
        return new DailyFocusRow(date, 0, 0, 0);
    }
}
