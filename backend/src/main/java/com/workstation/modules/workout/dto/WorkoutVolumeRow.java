package com.workstation.modules.workout.dto;

import java.math.BigDecimal;

/** 训练量查询的投影，start / end 由 Service 填 */
public record WorkoutVolumeRow(
        BigDecimal totalVolume,
        Long totalReps,
        Long totalSets,
        Long totalRecords) {
}
