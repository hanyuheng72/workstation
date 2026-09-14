package com.workstation.modules.ai.dto;

public record ChatResponse(
        Long conversationId,
        AiMessageVO userMessage,
        AiMessageVO assistantMessage) {
}
