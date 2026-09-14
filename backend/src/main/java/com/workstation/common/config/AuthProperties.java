package com.workstation.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 登录口令与令牌配置。
 * password 与 jwtSecret 来自 .env，任何日志与接口都不得输出其值。
 *
 * enabled 为 false 时整个登录环节被跳过：接口不再要求令牌，前端直接进入。
 * 部署到公网前请确认这个开关的状态——关掉之后，拿到网址的人就能读写全部数据。
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(Boolean enabled, String password, String jwtSecret, Integer jwtExpireDays) {

    public boolean isEnabled() {
        // 默认开启：漏配时宁可多要一次口令，也不要无声地敞开
        return enabled == null || enabled;
    }

    public boolean isConfigured() {
        return isEnabled() && password != null && !password.isBlank();
    }
}
