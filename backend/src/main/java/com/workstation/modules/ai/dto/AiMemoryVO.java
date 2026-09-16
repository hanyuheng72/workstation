package com.workstation.modules.ai.dto;

public record AiMemoryVO(
        Long id,
        String content,
        String category,
        String source,
        String createdAt) {
}
