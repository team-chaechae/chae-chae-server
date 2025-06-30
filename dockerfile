FROM amazoncorretto:17-alpine

WORKDIR /app

COPY build/libs/*.jar chae-chae.jar

ENTRYPOINT ["java", "-jar", "chae-chae.jar"]
