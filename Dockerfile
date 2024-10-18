FROM amazoncorretto:17-alpine
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul","-jar", "app.jar"]