package com.workstation.modules.workout.dto;

import jakarta.validation.constraints.Size;

public record ExerciseCreateRequest(
        @jakarta.validation.constraints.NotNull(message = "请选择部位")
        Long partId,

        @jakarta.validation.constraints.NotBlank(message = "请填写动作名称")
        @Size(max = 50, message = "动作名称最多 50 字")
        String name) {
}
