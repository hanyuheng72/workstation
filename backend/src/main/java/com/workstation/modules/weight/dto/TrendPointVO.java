package com.workstation.modules.weight.dto;

import java.math.BigDecimal;

/**
 * 趋势图上的一个点。
 * label 是给人看的（如 2026-09 / 9月13日），key 用于前端做 X 轴唯一标识。
 */
public record TrendPointVO(String key, String label, BigDecimal value) {
}
