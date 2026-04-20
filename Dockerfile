# ---- Build Stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
# Copy the entire backend-spring project
COPY backend-spring/ ./
RUN ./mvnw clean package -DskipTests
# Debug: list contents of /app/target
RUN ls -l /app/target

# ---- Run Stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
