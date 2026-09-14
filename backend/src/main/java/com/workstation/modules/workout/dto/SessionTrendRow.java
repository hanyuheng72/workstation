package com.workstation.modules.workout.dto;

import java.time.LocalDate;

/** 训练次数趋势的投影 */
public record SessionTrendRow(String label, Long value, LocalDate sampleDate) {
}
