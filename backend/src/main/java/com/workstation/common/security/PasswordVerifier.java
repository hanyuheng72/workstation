package com.workstation.common.security;

/**
 * 口令校验抽象。当前从 .env 读取单一口令；
 * 日后要换成多用户 + BCrypt 时，只需替换实现，过滤器链不用动。
 */
public interface PasswordVerifier {

    boolean matches(String rawPassword);
}
