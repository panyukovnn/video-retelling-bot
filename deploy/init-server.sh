#!/bin/bash

# Конфигурация
SSH_CONFIG=nvpnt
REMOTE_DIR=video-retelling-bot

# Проверка на наличие файлов рядом со скриптом
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"
APPLICATION_PROM_FILE="$SCRIPT_DIR/application-prom.yaml"

if [[ ! -f "$COMPOSE_FILE" || ! -f "$APPLICATION_PROM_FILE" ]]; then
  echo "Ошибка: не найден docker-compose.yml или application-prom.yaml рядом со скриптом."
  exit 1
fi

echo "Создание папки $REMOTE_DIR на сервере (если не существует)..."
ssh "$SSH_CONFIG" "mkdir -p $REMOTE_DIR"

echo "Копирование файлов на сервер..."
scp "$COMPOSE_FILE" "$APPLICATION_PROM_FILE" "$SSH_CONFIG:$REMOTE_DIR/"

echo "Готово. Файлы успешно отправлены."