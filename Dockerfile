# ---- Build stage ----
FROM gradle:8.6-jdk11 AS build
WORKDIR /home/gradle/project
COPY . .
RUN gradle clean bootJar --no-daemon

# ---- Runtime stage ----
FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
