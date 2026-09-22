package com.workstation.modules.focus.dto;

/**
 * 全时段累计。与 DailyFocusRow 分开是因为 MyBatis 的构造函数映射要求
 * 结果集列数与 record 的参数个数完全一致——累计查询没有日期列。
 * 聚合函数保证恒返回一行，没有记录时各项为 0。
 */
public record FocusTotalsRow(
        Integer successSeconds,
        Integer successCount,
        Integer failCount) {
}
