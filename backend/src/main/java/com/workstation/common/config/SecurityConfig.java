package com.workstation.common.config;

import com.workstation.common.security.JwtAuthenticationFilter;
import com.workstation.common.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   AuthProperties authProperties) throws Exception {
        boolean authEnabled = authProperties.isEnabled();

        http
                // 无 Cookie 会话，令牌走 Header，因此不需要 CSRF 保护
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    if (!authEnabled) {
                        // 登录已关闭：全部放行。
                        // 这条分支是为了「本地自用/私有部署」场景，公网部署请务必打开登录。
                        auth.anyRequest().permitAll();
                        return;
                    }
                    auth
                            .requestMatchers("/api/auth/login").permitAll()
                            // 前端靠它判断要不要显示登录页，所以必须放行
                            .requestMatchers("/api/auth/status").permitAll()
                            .requestMatchers("/actuator/health").permitAll()
                            .requestMatchers("/api/**").authenticated()
                            // 其余是前端静态资源与 SPA 路由，由 WebConfig 兜底
                            .anyRequest().permitAll();
                })
                .exceptionHandling(handling -> handling.authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
