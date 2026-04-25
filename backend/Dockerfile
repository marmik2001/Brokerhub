# Build stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
# Copy example properties to serve as the default properties file
RUN cp src/main/resources/application.properties.example src/main/resources/application.properties
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/demo-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
