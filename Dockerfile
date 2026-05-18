FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY . .

# [나중에 공통모듈 쓸 때 주석 해제]
# ARG GPR_USER
# ARG GPR_KEY
# ENV GPR_USER=${GPR_USER}
# ENV GPR_KEY=${GPR_KEY}

RUN chmod +x gradlew

# 현재는 순수하게 빌드만 진행 (공통모듈 미사용 버전)
RUN ./gradlew bootJar --no-daemon

# [나중에 공통모듈 쓸 때 주석 해제할 라인]
# RUN ./gradlew bootJar --no-daemon -Pgpr.user="${GPR_USER}" -Pgpr.key="${GPR_KEY}"

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
