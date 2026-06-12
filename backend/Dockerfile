# 水课管理系统 - 后端 Dockerfile
# 支持 Apple Silicon (ARM64) + Intel (x86_64)
# 构建: docker build -t shuike-backend:1.0 .

# ===== 第一阶段：编译 =====
FROM maven:3.9-amazoncorretto-8 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# ===== 第二阶段：运行 =====
FROM amazoncorretto:8-alpine
WORKDIR /app
# 安装中文字体
RUN apk add --no-cache fontconfig ttf-dejavu font-noto-cjk
COPY --from=builder /app/target/teacher-file-manager-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]
