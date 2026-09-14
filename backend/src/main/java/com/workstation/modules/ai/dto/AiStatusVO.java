package com.workstation.modules.ai.dto;

/** 只回报「配没配」，绝不回传 Key 本身，连前后几位都不回传 */
public record AiStatusVO(boolean configured, String model) {
}
