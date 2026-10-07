# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Копируем pom.xml и качаем зависимости. Docker кэширует этот слой.
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем код и собираем проект без тестов (тесты гоняет CI: им нужен Docker для Testcontainers)
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Запуск не от root: при взломе приложения у процесса нет прав на систему контейнера
RUN addgroup -S app && adduser -S app -G app

# Копируем только готовый артефакт из первой стадии
COPY --from=builder /app/target/*.jar app.jar
# Приложению нужна папка для логов (Logback пишет в logs/)
RUN mkdir logs && chown -R app:app /app

USER app
EXPOSE 8080

# MaxRAMPercentage: JVM берет долю от лимита памяти контейнера, а не от памяти хоста
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
