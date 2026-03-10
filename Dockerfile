FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:resolve
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/store-management.jar app.jar

# Verify the JAR contains PostgreSQL driver
RUN jar tf app.jar | grep -i "postgresql" | head -5 || echo "WARNING: No postgresql classes found!"

ENV PORT=8080
EXPOSE ${PORT}

# Use exec form so signals are properly forwarded
ENTRYPOINT ["java", "-jar", "app.jar"]

