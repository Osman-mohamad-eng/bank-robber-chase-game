FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl && addgroup -S bankheist && adduser -S bankheist -G bankheist
WORKDIR /app
COPY --from=build /workspace/target/bank-heist-api-1.0.0-SNAPSHOT.jar app.jar
USER bankheist
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
