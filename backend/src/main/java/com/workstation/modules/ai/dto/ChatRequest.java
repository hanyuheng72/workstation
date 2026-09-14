package com.workstation.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        /** 为空表示新开一个会话 */
        Long conversationId,

        @NotBlank(message = "说点什么吧")
        @Size(max = 500, message = "一次最多 500 字")
        String message) {
}
