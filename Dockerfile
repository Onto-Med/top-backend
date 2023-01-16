FROM maven:3-openjdk-11-slim AS build-stage
ARG GH_MAVEN_PKG_USER
ARG GH_MAVEN_PKG_AUTH_TOKEN
ENV GH_MAVEN_PKG_USER=$GH_MAVEN_PKG_USER
ENV GH_MAVEN_PKG_AUTH_TOKEN=$GH_MAVEN_PKG_AUTH_TOKEN
WORKDIR /app
COPY . .
COPY .mvn-ci.xml /root/.m2/settings.xml
RUN mvn package -B

FROM openjdk:11-jdk-slim AS production-stage
COPY --from=build-stage /app/target/*.jar /usr/src/top-backend/top-backend.jar
WORKDIR /usr/src/top-backend
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "top-backend.jar:\plugins\*", "org.springframework.boot.loader.PropertiesLauncher"]
