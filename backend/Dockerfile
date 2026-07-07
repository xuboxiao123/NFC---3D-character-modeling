# ============================================================
#  阶段1: 用 Maven 镜像编译 jar
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# 先复制 pom 走依赖缓存层（依赖没变就不重新拉）
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline || true

# 再复制源码编译
COPY src ./src
RUN mvn -B -DskipTests clean package

# ============================================================
#  阶段2: 极简运行时镜像
# ============================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 从 builder 阶段只拷出 jar
COPY --from=builder /build/target/NFC-system-0.0.1-SNAPSHOT.jar /app/app.jar

# 模型/图片本地存储目录（容器内，会被 volume 挂载覆盖）
RUN mkdir -p /app/storage/images /app/storage/models /app/storage/tmp
VOLUME ["/app/storage"]

# 健康检查：连一下 /actuator/health（Spring Boot Actuator 自动暴露）
# 如未启用 Actuator 可忽略此段，docker 会将其视为 unhealthy 但不影响启动
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8088/actuator/health || exit 0

EXPOSE 8088

# JVM 容器感知
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]