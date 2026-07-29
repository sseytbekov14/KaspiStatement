# Stage 1: Build the application using Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first to leverage Docker cache
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# We do not hardcode EXPOSE 8080 because Render sets the PORT dynamically.
# Spring Boot will automatically use the $PORT environment variable via application-prod.yml.

# The SPRING_PROFILES_ACTIVE environment variable will be set in the Render dashboard
# to activate application-prod.yml.
ENTRYPOINT ["java", "-jar", "app.jar"]
