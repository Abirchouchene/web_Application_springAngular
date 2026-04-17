# Single-stage Dockerfile using pre-built JAR from CI
FROM eclipse-temurin:17-jdk-jammy

# Copy the pre-built JAR file from the build stage
COPY target/*.jar /app/discovery.jar

# Set the working directory in the container
WORKDIR /app

# Start the application
CMD ["java", "-jar", "discovery.jar"]