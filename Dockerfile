# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app/backend-spring
COPY backend-spring/ ./
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests
RUN ls -l /app/backend-spring/target || (echo "Maven build failed, target directory missing" && exit 1)

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/backend-spring/target/backend-spring-0.1.0.jar /app/app.jar

# DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET must be set as environment variables
# in your Render service dashboard — do NOT hardcode credentials here.

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]