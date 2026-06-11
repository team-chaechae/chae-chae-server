FROM amazoncorretto:17

COPY build/libs/*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "/app.jar"]
