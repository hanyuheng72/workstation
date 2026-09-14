package com.workstation.modules.workout.dto;

public record ExerciseVO(
        Long id,
        Long partId,
        String name,
        Boolean isDefault) {
}
