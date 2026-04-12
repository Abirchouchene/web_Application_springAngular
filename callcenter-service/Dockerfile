# Step 1: Build the project using Maven
FROM maven:3.8.1-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Run the JAR using a lightweight OpenJDK image
FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/target/CallCenter-0.0.1-SNAPSHOT.jar call-center.jar

EXPOSE 8082
CMD ["java", "-jar", "call-center.jar"]
