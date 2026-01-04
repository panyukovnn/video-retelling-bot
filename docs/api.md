# API

## Основные эндпоинты

### Conveyor (обработка контента)

- `GET /conveyor/actuator/health` — Health-check приложения
- `POST /conveyor/consume` — Запуск обработки контента
  - Тело запроса: `ConsumeContentRequest`
- `POST /conveyor/update` — Обновление параметров обработки
  - Тело запроса: `UpdateParams`

### Content Consumer

- `POST /content/consume` — Запуск потребления контента
  - Тело запроса: `ConsumeContentRequest`

## Примеры DTO

### ConsumeContentRequest
```json
{
  "contentId": 123,
  "sourceType": "YOUTUBE",
  "url": "https://youtube.com/..."
}
```

### UpdateParams
```json
{
  "eventId": 456,
  "params": {
    "key": "value"
  }
}
```

## Обработка ошибок

- Все ошибки обрабатываются через `ConveyorExceptionHandler` и возвращаются в формате JSON с описанием причины.

## Swagger/OpenAPI

- Для тестирования и просмотра схемы API рекомендуется использовать Swagger (если включён в сборке). 