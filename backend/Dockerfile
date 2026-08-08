FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/knowledge-assistant-1.0.0.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/api/auth/login -X POST -H "Content-Type: application/json" -d '{}' || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]