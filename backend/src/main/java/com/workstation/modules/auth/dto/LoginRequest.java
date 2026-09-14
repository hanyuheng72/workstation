package com.workstation.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "请输入口令")
        String password) {
}
