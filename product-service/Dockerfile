FROM eclipse-temurin:21-jre
WORKDIR /app
COPY product-service/target/product-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
