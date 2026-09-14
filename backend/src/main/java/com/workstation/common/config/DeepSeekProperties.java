package com.workstation.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DeepSeek 配置。apiKey 只在本包内的 DeepSeekClient 读取，
 * 不得出现在任何 Controller 返回值或日志里。
 */
@ConfigurationProperties(prefix = "app.deepseek")
public record DeepSeekProperties(String apiKey, String baseUrl, String model) {

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String chatCompletionsUrl() {
        String base = (baseUrl == null || baseUrl.isBlank()) ? "https://api.deepseek.com" : baseUrl;
        return base.endsWith("/") ? base + "chat/completions" : base + "/chat/completions";
    }
}
