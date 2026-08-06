# ---------- build stage ----------
FROM gradle:8.10-jdk17 AS builder
WORKDIR /workspace

COPY settings.gradle build.gradle ./
COPY gradle ./gradle
# 의존성 레이어 캐싱 (소스 변경 시 재다운로드 방지)
RUN gradle dependencies --no-daemon || true

COPY src ./src
RUN gradle clean bootJar --no-daemon -x test

# ---------- runtime stage ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

ENV TZ=Asia/Seoul
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Seoul -Dfile.encoding=UTF-8"

RUN groupadd -r app && useradd -r -g app app
COPY --from=builder /workspace/build/libs/*.jar app.jar
RUN chown app:app app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
