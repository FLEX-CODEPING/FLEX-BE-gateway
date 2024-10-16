# 빌드 스테이지
FROM amazoncorretto:17-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew build -x test  # Gradle을 사용하는 경우

# JAR 추출 스테이지
FROM amazoncorretto:17-alpine AS extractor
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract

# 최종 스테이지
FROM amazoncorretto:17-alpine
ENV spring.profiles.active=dev
WORKDIR /app
COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app/spring-boot-loader/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/application/ ./
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "org.springframework.boot.loader.JarLauncher"]