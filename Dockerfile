# Stage 1: Build

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app/backend-spring
# Copy the entire backend-spring project (all files, including wrapper and resources)
COPY backend-spring/ ./
# Debug: list all files after copy
RUN ls -l /app/backend-spring && ls -l /app/backend-spring/src || true
# Ensure wrapper is executable
RUN chmod +x mvnw
# Build and repackage to ensure a runnable JAR (show debug output)
RUN ./mvnw clean package -DskipTests -X
# Debug: show contents of /app/backend-spring/target, fail if missing
RUN ls -l /app/backend-spring/target || (echo "Maven build failed, target directory missing" && exit 1)

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app
# Copy the built jar from the build stage (be explicit)
COPY --from=build /app/backend-spring/target/backend-spring-0.1.0.jar /app/app.jar
RUN ls -l /app

# --- Aiven MySQL ENV template for local testing ---
# Replace these values with your actual Aiven credentials or override at runtime
ENV DB_URL="jdbc:mysql://aiven-mysql-host:3306/dbname?useSSL=true"
ENV DB_USER="aivenuser"
ENV DB_PASSWORD="aivenpassword"
# -----------------------------------------------

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]