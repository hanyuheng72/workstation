package com.workstation.modules.workout.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WorkoutVO(
        Long id,
        LocalDate recordDate,
        Long partId,
        String partName,
        String partCode,
        boolean cardio,
        Long exerciseId,
        String exerciseName,
        String note,
        List<WorkoutSetVO> sets,
        /** 力量：Σ(重量 × 次数)；有氧：0 */
        BigDecimal totalVolume,
        /** 力量：最大重量；有氧：null */
        BigDecimal maxWeight,
        int totalReps) {
}
