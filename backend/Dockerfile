# 个人工作台 —— 后端镜像
#
# 两段式构建：第一段用带 Maven 的镜像编译，第二段只留 JRE 和 jar，
# 最终镜像不含源码与 Maven 缓存。
#
# 构建：  docker build -t workstation-backend ./backend
# 运行：  docker run -p 8080:8080 --env-file backend/.env workstation-backend

# ---------- 构建 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先只拷 pom 并预拉依赖：源码改动时这一层的缓存不会失效，重建快很多。
# 后面接 `|| true` 是因为 dependency:go-offline 偶尔会因某个插件的元数据解析失败而报错，
# 而它只是加速手段——真正的构建会补齐缺的依赖，不该因为它挂掉整个镜像。
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline || true

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# ---------- 运行 ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 以非 root 运行。云平台的容器默认可写层很小，日志也走 stdout，不需要写权限
RUN addgroup -S app && adduser -S app -G app
# Spring Boot 打包后 target 下会同时有 xxx.jar 和 xxx.jar.original，
# 但 *.jar 只匹配前者（后者以 .original 结尾），不会拷错。
COPY --from=build /build/target/*.jar app.jar
RUN chown -R app:app /app
USER app

# 云平台上由平台注入 PORT；本地 docker run 时用 8080
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

# 免费档容器内存通常只有 512MB，让 JVM 按容器可用内存的 75% 自己定堆大小，
# 不写死 -Xmx，避免平台调整规格后又要改镜像。
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+UseSerialGC", "-jar", "/app/app.jar"]
