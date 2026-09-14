package com.workstation.modules.workout.dto;

import java.math.BigDecimal;

public record WorkoutSetVO(
        Long id,
        Integer setIndex,
        BigDecimal weightKg,
        Integer reps,
        BigDecimal durationMin,
        BigDecimal distanceKm) {
}
