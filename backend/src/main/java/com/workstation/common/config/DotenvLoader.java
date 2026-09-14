package com.workstation.common.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 把 backend/.env 载入为系统属性，供 application.yml 的 ${...} 占位符解析。
 * 已存在的系统属性不会被覆盖，方便临时用环境变量顶掉 .env 里的值。
 */
public final class DotenvLoader {

    private static final Logger log = LoggerFactory.getLogger(DotenvLoader.class);

    private DotenvLoader() {
    }

    public static void load() {
        Path dir = resolveDir();
        if (dir == null) {
            log.info("未找到 .env 文件，改为只使用系统环境变量");
            return;
        }

        Dotenv dotenv = Dotenv.configure()
                .directory(dir.toString())
                .ignoreIfMissing()
                .load();

        int loaded = 0;
        for (DotenvEntry entry : dotenv.entries()) {
            if (System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
                loaded++;
            }
        }
        // 只报告条数，绝不打印任何一项的值
        log.info("已从 {} 载入 {} 项环境变量", dir.resolve(".env").toAbsolutePath().normalize(), loaded);
    }

    /** 支持从项目根目录或 backend 目录启动。 */
    private static Path resolveDir() {
        if (Files.isRegularFile(Path.of(".env"))) {
            return Path.of(".");
        }
        if (Files.isRegularFile(Path.of("backend", ".env"))) {
            return Path.of("backend");
        }
        return null;
    }
}
