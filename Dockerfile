# syntax=docker/dockerfile:1
FROM maven:3-eclipse-temurin-21-alpine AS build-stage
WORKDIR /app
COPY . .
COPY .mvn-ci.xml /root/.m2/settings.xml
RUN --mount=type=cache,target=/root/.m2/repository \
    --mount=type=secret,id=GH_MAVEN_PKG_USER \
    --mount=type=secret,id=GH_MAVEN_PKG_AUTH_TOKEN \
    GH_MAVEN_PKG_USER=$(cat /run/secrets/GH_MAVEN_PKG_USER) \
    GH_MAVEN_PKG_AUTH_TOKEN=$(cat /run/secrets/GH_MAVEN_PKG_AUTH_TOKEN) \
    mvn package -B --no-transfer-progress -DskipTests=true

FROM eclipse-temurin:21-jre-alpine AS production-stage
ENV LOADER_PATH=/plugins
COPY --from=build-stage /app/target/*.jar /usr/src/top-backend/top-backend.jar
WORKDIR /usr/src/top-backend
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "top-backend.jar", "org.springframework.boot.loader.launch.JarLauncher"]
