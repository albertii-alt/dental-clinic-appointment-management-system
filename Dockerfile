# ---- Build Stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
# Copy Maven project files from backend-spring
COPY backend-spring/pom.xml ./
COPY backend-spring/.mvn ./.mvn
COPY backend-spring/mvnw ./mvnw
COPY backend-spring/mvnw.cmd ./mvnw.cmd
# Copy the rest of the source
COPY backend-spring/src ./src
COPY backend-spring/src/main/resources ./src/main/resources
RUN ./mvnw clean package -DskipTests

# ---- Run Stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
