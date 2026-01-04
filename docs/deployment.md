# Деплой и настройка

## Требования
- Docker, Docker Compose
- Сервер с доступом к YouTube (желательно зарубежный)
- Доступ к Telegram Bot API
- Переменные окружения и секреты (см. ниже)

## Шаги деплоя

### 1. Подготовка пользователя и окружения

1. Подключитесь к серверу: `ssh root@<ip>`
2. Создайте группу и пользователя:
   ```bash
   groupadd panyukovnn
   useradd -d /home/tech -m -s /bin/bash -g panyukovnn tech
   passwd tech
   ```
3. Настройте SSH для пользователя `tech`.
4. Дайте права на Docker:
   ```bash
   sudo usermod -aG docker tech
   ```

### 2. Подготовка файлов

- База данных разворачивается в Docker на том же сервере.
- Конфигурация для подключения к БД хранится в `/etc/common-config/config.env`.

1. Создайте `docker-compose.yml` и `application-prom.yaml` в папке `/deploy`.
2. Создайте файл `config.env` с секретами и переменными окружения.
3. Выполните скрипт инициализации:
   ```bash
   cd deploy
   ./init-server.sh
   ```
4. Выполните аутентификацию в Github Packages:
   ```bash
   echo YOUR_GITHUB_TOKEN | docker login ghcr.io -u YOUR_USERNAME --password-stdin
   ```
   Где `YOUR_GITHUB_TOKEN` — PAT с правами `read:packages`.

### 3. Запуск и обновление

1. Залейте изменения в ветку `main`.
2. Создайте тег (release) в репозитории.
3. Дождитесь уведомления в Telegram о сборке.
4. Запустите workflow `deploy-tag` в GitHub Actions.

## Структура docker-compose.yml

```yaml
version: '3.3'
services:
  retelling-bot:
    image: ghcr.io/panyukovnn/retelling-bot:${APP_TAG}
    container_name: retelling-bot
    ports:
      - 8082:8082
    extra_hosts:
      - "host.docker.internal:host-gateway"
    environment:
      SPRING_PROFILES_ACTIVE: prom
    env_file:
      - /etc/common-config/config.env
    volumes:
      - ./application-prom.yaml:/application-prom.yaml
```

## Пример переменных окружения (application-prom.yaml)

- `DEEPSEEK_API_KEY` — ключ для LLM
- `BOT_NAME`, `BOT_TOKEN`, `BOT_ADMIN_IDS` — параметры Telegram-бота
- `PUBLISHING_CHAT_ID`, `RATE_TG_TOPIC_ID`, ... — параметры публикации и топиков
- Промпты для LLM (см. файл)

## Скрипт инициализации (init-server.sh)

- Проверяет наличие необходимых файлов
- Создаёт папку на сервере
- Копирует файлы для деплоя

---

## CI/CD

- Используется GitHub Actions (`.github/workflows/deploy-tag.yaml`)
- Автоматический деплой по тегу
- Health-check приложения
- Telegram-уведомления о статусе деплоя 