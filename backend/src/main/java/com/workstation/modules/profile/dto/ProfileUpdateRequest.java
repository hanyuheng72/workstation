package com.workstation.modules.profile.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProfileUpdateRequest(
        @Size(max = 50, message = "昵称最多 50 字")
        String nickname,

        @DecimalMin(value = "100.0", message = "身高看起来不太对，请检查")
        @DecimalMax(value = "250.0", message = "身高看起来不太对，请检查")
        BigDecimal heightCm,

        Boolean aiEnabled,

        @Size(max = 50, message = "模型名过长")
        String aiModel) {
}
