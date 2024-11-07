FROM amazoncorretto:17-alpine

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=prod

COPY build/libs/*.jar app.jar

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "app.jar"]