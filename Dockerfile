# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy the wrapper and pom first (helps with caching)
COPY backend-spring/mvnw .
COPY backend-spring/.mvn .mvn
COPY backend-spring/pom.xml .
COPY backend-spring/src src

# Ensure line endings are LF (useful if developing on Windows) 
# and the wrapper is executable
RUN tr -d '\r' < mvnw > mvnw_unix && mv mvnw_unix mvnw
RUN chmod +x mvnw

# Run the build - this should now take much longer than 0.1s
RUN ./mvnw clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar from the build stage
# Using a wildcard helps if the version number changes
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]