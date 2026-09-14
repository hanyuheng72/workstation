package com.workstation;

import com.workstation.common.config.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 排除 UserDetailsServiceAutoConfiguration：本项目的登录完全走 JWT，
 * 不定义 UserDetailsService。不排除的话启动日志会打印一句
 * 「Using generated security password: ...」，看起来像配置漏了，其实无关紧要。
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class WorkstationApplication {

    public static void main(String[] args) {
        // 必须在 run() 之前：application.yml 用 ${DB_URL} 这类占位符引用 .env 的值
        DotenvLoader.load();
        SpringApplication.run(WorkstationApplication.class, args);
    }
}
