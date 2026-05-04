FROM maven:3.9.15-eclipse-temurin-25-alpine AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .
COPY pom.xml .
COPY src src

RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /
VOLUME /tmp
COPY --from=build /workspace/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
