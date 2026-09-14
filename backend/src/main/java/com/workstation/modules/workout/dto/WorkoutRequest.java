package com.workstation.modules.workout.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 部位由所选动作决定，前端不需要（也不应该）单独传 partId，避免两者不一致。
 */
public record WorkoutRequest(
        LocalDate recordDate,

        @NotNull(message = "请选择动作")
        Long exerciseId,

        @NotEmpty(message = "至少记录一组")
        @Size(max = 50, message = "单次最多记录 50 组")
        List<WorkoutSetRequest> sets,

        @Size(max = 255, message = "备注最多 255 字")
        String note) {
}
