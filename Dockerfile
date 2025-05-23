# Étape 1 : Build avec Maven
FROM maven:3.9.3-eclipse-temurin-17 AS build
WORKDIR /app

# Copier tous les fichiers du projet
COPY . .

# Construire le projet
RUN mvn clean package -DskipTests

# Étape 2 : Exécuter avec JDK 17
FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
WORKDIR /app

# Copier le jar depuis l'étape précédente
COPY --from=build /app/target/security-0.0.1-SNAPSHOT.jar app.jar

# Exposer le port
EXPOSE 8080

# Lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]