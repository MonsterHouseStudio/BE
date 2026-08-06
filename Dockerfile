# ---------- build stage ----------
# gradle:<ver>-jdk<ver> 이미지 대신 JDK 이미지 + 프로젝트 래퍼를 쓰는 이유:
#   래퍼(gradle-wrapper.properties)가 Gradle 버전을 고정하고 있는데,
#   이미지에 박힌 Gradle 버전이 다르면 로컬과 CI 가 서로 다른 Gradle 로 빌드하게 됩니다.
#   래퍼를 쓰면 어디서 빌드하든 같은 버전이 보장됩니다.
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

COPY gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY settings.gradle build.gradle ./
# 의존성 레이어 캐싱 (소스 변경 시 재다운로드 방지)
RUN ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew clean bootJar --no-daemon -x test

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

ENV TZ=Asia/Seoul
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Seoul -Dfile.encoding=UTF-8"

RUN groupadd -r app && useradd -r -g app app
COPY --from=builder /workspace/build/libs/*.jar app.jar
RUN chown app:app app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
