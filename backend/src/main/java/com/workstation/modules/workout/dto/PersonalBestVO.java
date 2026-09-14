package com.workstation.modules.workout.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 某个动作的历史最大重量（PR），以及达成时的次数与日期 */
public record PersonalBestVO(
        Long exerciseId,
        String exerciseName,
        String partName,
        BigDecimal maxWeight,
        Integer reps,
        LocalDate achievedDate) {
}
