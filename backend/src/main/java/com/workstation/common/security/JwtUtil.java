package com.workstation.common.security;

import com.workstation.common.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtUtil {

    private static final String SUBJECT = "workstation";

    /** 登录关闭时不签发也不校验，密钥可以为空 */
    private final boolean enabled;
    private final SecretKey key;
    private final Duration ttl;

    public JwtUtil(AuthProperties properties) {
        this.enabled = properties.isEnabled();
        int days = properties.jwtExpireDays() == null ? 30 : Math.max(1, properties.jwtExpireDays());
        this.ttl = Duration.ofDays(days);

        String secret = properties.jwtSecret();
        if (!enabled) {
            this.key = null;
            return;
        }
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            // 快速失败：密钥不合格时宁可起不来，也不要发出可被伪造的令牌
            throw new IllegalStateException(
                    "JWT_SECRET 未配置或长度不足 32 字节，请在 backend/.env 中设置一个足够长的随机串");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String issue() {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(SUBJECT)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public long expiresAtMillis() {
        return Instant.now().plus(ttl).toEpochMilli();
    }

    /** 签名无效或已过期时返回空，由调用方当作未登录处理 */
    public Optional<Claims> parse(String token) {
        if (!enabled) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
