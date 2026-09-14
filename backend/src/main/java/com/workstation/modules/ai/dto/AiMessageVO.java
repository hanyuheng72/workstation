package com.workstation.modules.ai.dto;

import com.workstation.modules.ai.entity.ActionStatus;

import java.time.LocalDateTime;

public record AiMessageVO(
        Long id,
        String role,
        String content,
        String intent,
        ActionStatus actionStatus,
        /** 只是给人看的预览文案，不是要落库的数据 */
        String draftPreview,
        /** 确认后实际记了什么 */
        String resultSummary,
        LocalDateTime createdAt) {
}
