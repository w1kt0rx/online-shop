FROM eclipse-temurin:21-jre
WORKDIR /app
COPY user-interaction-service/target/user-interaction-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
