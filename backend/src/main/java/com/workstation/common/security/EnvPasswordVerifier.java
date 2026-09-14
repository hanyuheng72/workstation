package com.workstation.common.security;

import com.workstation.common.config.AuthProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class EnvPasswordVerifier implements PasswordVerifier {

    private final byte[] expected;

    public EnvPasswordVerifier(AuthProperties properties) {
        String password = properties.password();
        this.expected = (password == null) ? new byte[0] : password.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean matches(String rawPassword) {
        // 未配置口令时一律拒绝，避免漏配 .env 就变成人人可进
        if (expected.length == 0) {
            return false;
        }
        byte[] candidate = (rawPassword == null) ? new byte[0] : rawPassword.getBytes(StandardCharsets.UTF_8);
        // 常量时间比较，避免按字符逐位比较泄露口令长度与内容
        return MessageDigest.isEqual(expected, candidate);
    }
}
