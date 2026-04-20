# ---- Build Stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 1. Copy the project files
COPY backend-spring/ ./

# 2. Fix permissions for the Maven wrapper
RUN chmod +x mvnw

# 3. Clean build - the plugin in your pom.xml handles the rest
RUN ./mvnw clean package -DskipTests

# ---- Run Stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# 4. Use a wildcard to copy the executable JAR
# This ignores the .jar.original file you saw in your logs
COPY --from=build /app/target/backend-spring-*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]