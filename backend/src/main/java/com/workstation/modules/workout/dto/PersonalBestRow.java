package com.workstation.modules.workout.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** PR 查询的投影，字段对应 SQL 别名 */
public record PersonalBestRow(
        Long exerciseId,
        BigDecimal maxWeight,
        Integer reps,
        LocalDate achievedDate) {
}
