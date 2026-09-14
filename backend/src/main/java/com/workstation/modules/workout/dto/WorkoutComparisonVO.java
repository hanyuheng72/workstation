package com.workstation.modules.workout.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 本次训练与「上一次同一动作」的对比，用来给出「重量较上次提升 2.5kg」这类提示。
 * 没有上一次记录时 hasPrevious = false，前端不做任何比较展示。
 */
public record WorkoutComparisonVO(
        boolean hasPrevious,
        LocalDate previousDate,
        BigDecimal previousMaxWeight,
        BigDecimal currentMaxWeight,
        BigDecimal weightDelta,
        BigDecimal previousVolume,
        BigDecimal currentVolume,
        BigDecimal volumeDelta,
        String message) {

    public static WorkoutComparisonVO none() {
        return new WorkoutComparisonVO(false, null, null, null, null, null, null, null, null);
    }
}
