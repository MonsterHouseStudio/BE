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

# ★ USER 는 반드시 숫자 UID 로 지정합니다.
#   이름("app")으로 두면 쿠버네티스가 securityContext.runAsNonRoot 를 검증하지 못해
#   파드가 CreateContainerConfigError 로 뜨지 않습니다:
#     "container has runAsNonRoot and image has non-numeric user (app),
#      cannot verify user is non-root"
#   kubelet 은 이미지 메타데이터의 USER 문자열만 보고 판단하므로,
#   /etc/passwd 에 계정이 있어도 이름이면 root 가 아님을 확인할 방법이 없습니다.
RUN groupadd -r -g 10001 app && useradd -r -u 10001 -g app app
COPY --from=builder /workspace/build/libs/*.jar app.jar
RUN chown 10001:10001 app.jar
USER 10001

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
