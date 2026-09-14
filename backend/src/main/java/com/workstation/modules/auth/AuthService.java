package com.workstation.modules.auth;

import com.workstation.common.config.AuthProperties;
import com.workstation.common.exception.BusinessException;
import com.workstation.common.result.ErrorCode;
import com.workstation.common.security.JwtUtil;
import com.workstation.common.security.PasswordVerifier;
import com.workstation.modules.auth.dto.LoginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthProperties authProperties;
    private final PasswordVerifier passwordVerifier;
    private final JwtUtil jwtUtil;

    public AuthService(AuthProperties authProperties, PasswordVerifier passwordVerifier, JwtUtil jwtUtil) {
        this.authProperties = authProperties;
        this.passwordVerifier = passwordVerifier;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(String rawPassword) {
        // 关闭登录的部署上 JWT 密钥是空的，走到签发那步会直接 NPE。
        // 登录本身在这里就没有意义，明确拒绝比崩掉清楚。
        if (!authProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前部署未启用登录");
        }
        if (!passwordVerifier.matches(rawPassword)) {
            // 只记录失败事实，不记录尝试的口令内容
            log.warn("登录失败：口令不正确");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "口令不正确");
        }
        return new LoginResponse(jwtUtil.issue(), jwtUtil.expiresAtMillis());
    }
}
