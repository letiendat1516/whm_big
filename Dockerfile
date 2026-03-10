FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:resolve
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/store-management.jar app.jar

ENV PORT=8080
EXPOSE ${PORT}
CMD ["java", "-jar", "app.jar"]

