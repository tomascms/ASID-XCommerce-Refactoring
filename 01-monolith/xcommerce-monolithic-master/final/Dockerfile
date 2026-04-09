# Dockerfile para XCommerce Monólito
# Não altera código, só empacota a compilação existente

# Stage 1: Build
FROM gradle:7.6-jdk8 as builder

WORKDIR /app

# Copiar build files
COPY build.gradle .
COPY settings.gradle .
COPY gradle/ ./gradle/
COPY gradlew .
COPY gradlew.bat .

# Copiar source
COPY src/ ./src/

# Compilar
RUN chmod +x gradlew && ./gradlew build -x test --no-daemon

# Stage 2: Runtime
FROM openjdk:8-jre-slim

WORKDIR /app

# Copiar JAR da build anterior
COPY --from=builder /app/build/libs/*.jar app.jar

# Spring Boot port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=10s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run
ENTRYPOINT ["java", "-jar", "app.jar", \
  "--server.port=8080", \
  "--spring.datasource.url=jdbc:postgresql://postgres:5432/xcommerce", \
  "--spring.datasource.username=postgres", \
  "--spring.datasource.password=postgres", \
  "--spring.jpa.hibernate.ddl-auto=update"]
