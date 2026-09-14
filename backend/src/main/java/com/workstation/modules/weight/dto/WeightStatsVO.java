package com.workstation.modules.weight.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 体重统计。current 恒取最新一条记录（本项目不设目标体重）。
 * 没有任何记录时各字段为 null，前端据此显示空状态，不要造数。
 */
public record WeightStatsVO(
        BigDecimal current,
        LocalDate currentDate,
        BigDecimal max,
        BigDecimal min,
        BigDecimal average,
        /** 最新一条与上一条的差值，只有一条记录时为 null */
        BigDecimal latestDelta,
        long recordCount) {

    public static WeightStatsVO empty() {
        return new WeightStatsVO(null, null, null, null, null, null, 0);
    }
}
