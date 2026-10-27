FROM eclipse-temurin:21-jre
WORKDIR /app
COPY invoice-service/target/invoice-service-*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
