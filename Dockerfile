# Étape 1 : builder
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copier les fichiers du projet
COPY pom.xml .
COPY src ./src

# Compiler le projet et générer le .jar
RUN mvn clean package -DskipTests

# Étape 2 : image de runtime
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copier le .jar généré depuis l'étape précédente
COPY --from=builder /app/target/*.jar app.jar

# Exposer le port (adapter selon ton application)
EXPOSE 8080

# Lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
