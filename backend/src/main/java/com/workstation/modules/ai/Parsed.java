package com.workstation.modules.ai;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 一次解析的结果。
 * intent 为 null 表示这句话不含可记录的动作，只是一句普通对话。
 */
public record Parsed(
        String reply,
        String intent,
        JsonNode payload,
        int promptTokens,
        int completionTokens) {

    public boolean hasAction() {
        return intent != null && !intent.isBlank() && payload != null && !payload.isNull();
    }
}
