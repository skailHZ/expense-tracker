# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Копируем pom.xml и качаем зависимости. Docker кэширует этот слой.
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем код и собираем проект без тестов
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Копируем только готовый артефакт из первой стадии
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]