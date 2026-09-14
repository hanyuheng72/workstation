package com.workstation.modules.workout.dto;

import java.math.BigDecimal;

/**
 * 力量组填 weightKg + reps；有氧组填 durationMin 或 distanceKm。
 * 具体哪一组字段必填由所属部位决定，在 Service 层校验。
 */
public record WorkoutSetRequest(
        BigDecimal weightKg,
        Integer reps,
        BigDecimal durationMin,
        BigDecimal distanceKm) {
}
