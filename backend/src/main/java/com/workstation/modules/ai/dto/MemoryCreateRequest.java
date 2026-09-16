package com.workstation.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemoryCreateRequest(
        @NotBlank(message = "请填写要记住的内容")
        @Size(max = 500, message = "记忆最多 500 字")
        String content,

        String category) {
}
