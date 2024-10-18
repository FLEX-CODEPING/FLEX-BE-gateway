# 런타임 스테이지
FROM amazoncorretto:17-alpine
ENV spring.profiles.active=dev
WORKDIR /app

# JAR 파일 복사 (Jenkins에서 빌드된 JAR 파일을 사용)
COPY target/*.jar app.jar

COPY .env .env
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul","-jar", "app.jar"]