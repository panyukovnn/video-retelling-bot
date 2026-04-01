# Используем официальный образ Gradle с JDK
FROM gradle:8.14-jdk21 AS build_image

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем файлы конфигурации для кеширования зависимостей
COPY build.gradle settings.gradle linters.gradle ./

# Копируем исходный код
COPY src ./src

# Собираем приложение, используя BuildKit secrets для безопасной передачи токена
RUN --mount=type=secret,id=gh_token \
    GITHUB_PACKAGES_READ_TOKEN=$(cat /run/secrets/gh_token) gradle build -x test --no-daemon

# Runtime образ
FROM eclipse-temurin:21-jre

# Копируем собранный jar файл
COPY --from=build_image /app/build/libs/*.jar ./app.jar

# Устанавливаем часовой пояс
ENV TZ=Europe/Moscow

# Открываем порт
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "./app.jar"]