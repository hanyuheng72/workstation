package com.workstation.modules.ai.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AiSummaryVO(
        LocalDate date,
        String content,
        LocalDateTime generatedAt,
        /** true 表示直接读的缓存，没有重新调用模型 */
        boolean cached) {
}
