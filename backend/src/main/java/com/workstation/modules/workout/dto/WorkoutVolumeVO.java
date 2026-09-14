package com.workstation.modules.workout.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 训练量。totalVolume / totalReps 只统计力量组——
 * 有氧组的重量与次数是 null，本来就不该计入容量。
 */
public record WorkoutVolumeVO(
        LocalDate start,
        LocalDate end,
        BigDecimal totalVolume,
        long totalReps,
        long totalSets,
        long totalRecords) {
}
