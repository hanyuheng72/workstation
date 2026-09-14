package com.workstation.modules.finance.dto;

import java.math.BigDecimal;

/**
 * 按周期（日或月）聚合的收支。
 * 收入与支出放在同一个点里，前端画柱状图时可以直接并排。
 * 没有记录的周期也会补一个收支都是 0 的点，避免趋势图出现断点。
 */
public record PeriodAmountVO(String key, String label, BigDecimal income, BigDecimal expense) {
}
