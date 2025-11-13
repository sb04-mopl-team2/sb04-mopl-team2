# ====== 1단계: 빌드 스테이지 ======
FROM gradle:8.7-jdk17 AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 캐시 경로 설정
ENV GRADLE_USER_HOME=/home/gradle/.gradle

# 권한 설정 (gradle 유저가 디렉토리를 쓸 수 있게)
RUN mkdir -p ${GRADLE_USER_HOME} && chown -R gradle:gradle /home/gradle /app

# gradle 유저로 전환
USER gradle

# Gradle Wrapper 및 설정 파일 복사
COPY --chown=gradle:gradle gradlew ./
COPY --chown=gradle:gradle gradle ./gradle
COPY --chown=gradle:gradle settings.gradle build.gradle ./

# 의존성 미리 다운로드 (실패해도 무시)
RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon dependencies || true

# 소스 복사 및 빌드
COPY --chown=gradle:gradle src ./src
RUN ./gradlew clean build --no-daemon -x test

# ====== 2단계: 런타임 스테이지 ======
FROM amazoncorretto:17-alpine3.21

# 작업 디렉토리 설정
WORKDIR /app

# 환경 변수 (Spring, JVM 설정 등)
ENV SPRING_PROFILES_ACTIVE=prod \
    JVM_OPTS=""

# 런타임 실행용 app 유저 생성 (보안용)
RUN addgroup -S app && adduser -S app -G app

# 빌드 결과 JAR 복사
# 빌드 아티팩트를 app 소유로 복사
COPY --from=builder --chown=app:app /app/build/libs/*.jar app.jar

USER app

# 포트 노출
EXPOSE 80

# 실행 명령
ENTRYPOINT ["sh", "-c", "java ${JVM_OPTS} -jar app.jar"]
