# Stage 1: Build

FROM eclipse-temurin:21-jdk AS build
	WORKDIR /app/backend-spring
# Copy the entire backend-spring project (preserves structure)
COPY backend-spring/ ./
# Ensure wrapper is executable
RUN chmod +x mvnw
# Build and repackage to ensure a runnable JAR
RUN ./mvnw clean package -DskipTests
# Debug: show contents of /app/target
	RUN ls -l /app/backend-spring/target

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app
# Copy the built jar from the build stage
	COPY --from=build /app/backend-spring/target/*.jar /app/app.jar
RUN ls -l /app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]