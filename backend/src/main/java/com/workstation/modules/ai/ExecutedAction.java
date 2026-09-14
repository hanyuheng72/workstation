package com.workstation.modules.ai;

/**
 * 动作落库后的结果。summary 是给界面显示的一句「实际记了什么」。
 */
public record ExecutedAction(String entityType, Long entityId, String summary) {
}
