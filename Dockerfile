# Dockerfile
# Этап сборки
FROM maven:3.8.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline  # Кэшируем зависимости
COPY src ./src
RUN mvn clean package -DskipTests  # Собираем JAR

# Финальный образ
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]