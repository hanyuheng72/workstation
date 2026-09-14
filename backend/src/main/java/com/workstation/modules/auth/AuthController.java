package com.workstation.modules.auth;

import com.workstation.common.config.AuthProperties;
import com.workstation.common.result.ApiResponse;
import com.workstation.modules.auth.dto.AuthStatusVO;
import com.workstation.modules.auth.dto.LoginRequest;
import com.workstation.modules.auth.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthProperties authProperties;

    public AuthController(AuthService authService, AuthProperties authProperties) {
        this.authService = authService;
        this.authProperties = authProperties;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request.password()));
    }

    /**
     * 前端启动时先问这个：登录关掉的部署直接进首页，不用先跳一次登录页。
     */
    @GetMapping("/status")
    public ApiResponse<AuthStatusVO> status() {
        return ApiResponse.ok(new AuthStatusVO(authProperties.isEnabled()));
    }

    /**
     * 前端启动时调用：能走到这里说明令牌有效，否则会被 Security 拦成 401。
     */
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        return ApiResponse.ok(Map.of("authenticated", true));
    }
}
