package com.workstation.modules.weight.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 趋势查询的投影，字段名与 SQL 别名一一对应 */
public record WeightTrendRow(String label, BigDecimal value, LocalDate sampleDate) {
}
