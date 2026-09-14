package com.workstation.modules.workout.dto;

public record ExercisePartVO(
        Long id,
        String code,
        String name,
        Integer sortOrder,
        boolean cardio,
        long sessionCount,
        long exerciseCount) {
}
