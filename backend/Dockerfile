# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
RUN apk add --no-cache maven
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true -B

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /app/target/mini-trello-enterprise.jar app.jar
EXPOSE 8080

# Lets `docker compose ps` / orchestrators know when the JVM is actually
# ready to serve traffic, not just that the process has started — Spring
# Boot can take several seconds after process start before the app
# context (and Flyway migrations) finish.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
