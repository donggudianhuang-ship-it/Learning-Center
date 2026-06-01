# ============================================================
#  Stage 1: Maven 构建
# ============================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build
COPY pom.xml .
# 先下载依赖（利用 Docker 层缓存）
RUN mvn dependency:go-offline -B -q 2>/dev/null || true

COPY src ./src
RUN mvn package -DskipTests -B -q

# ============================================================
#  Stage 2: 运行镜像
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# 安全：非 root 运行
RUN addgroup -S app && adduser -S app -G app
USER app

WORKDIR /app

# JVM 参数（可通过 JAVA_OPTS 环境变量覆盖）
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
# 默认使用生产配置
ENV SPRING_PROFILES_ACTIVE=prod

# 复制构建产物
COPY --from=builder /build/target/*.jar app.jar

# OCR 训练数据（从项目复制，也可运行时挂载）
COPY --chown=app:app tessdata /app/tessdata

EXPOSE 8081

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
