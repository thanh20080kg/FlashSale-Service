# syntax=docker/dockerfile:1

# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,id=flashsale-maven-repository,target=/root/.m2 \
    mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,id=flashsale-maven-repository,target=/root/.m2 \
    mvn -B -q -DskipTests package

# ---- runtime ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/*.jar /app/app.jar
USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
