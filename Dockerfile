FROM amazoncorretto:17-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=dev
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "app.jar"]