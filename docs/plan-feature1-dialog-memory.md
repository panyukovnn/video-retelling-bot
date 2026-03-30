# Feature 1: Диалоговая память + Q&A режим

## Описание

Вместо одноразового пересказа бот переходит в агентный режим:
- LLM имеет один `@Tool` для загрузки субтитров
- История диалога хранится в БД на каждого пользователя
- После пересказа пользователь может задавать вопросы по содержанию видео
- Новая ссылка сбрасывает диалог; при превышении 200к токенов диалог завершается автоматически

---

## Чеклист реализации

### 1.1 Миграции БД

- [x] Создать миграцию `v1.1.0/changelog.yml`
- [x] Таблица `dialog_sessions`:
  - `id` UUID PK
  - `client_id` UUID FK → `clients.id`
  - `video_url` VARCHAR
  - `status` VARCHAR — значения: `ACTIVE`, `CLOSED`
  - `closed_at` TIMESTAMP NULLABLE
  - аудит: `create_time`, `create_user`, `last_update_time`, `last_update_user` (как в `clients`)
- [x] Таблица `dialog_messages`:
  - `id` UUID PK
  - `session_id` UUID FK → `dialog_sessions.id`
  - `role` VARCHAR — значения: `USER`, `ASSISTANT`, `TOOL`
  - `content` TEXT
  - аудит: `create_time`, `create_user`, `last_update_time`, `last_update_user`

---

### 1.2 JPA-модели и репозитории

- [x] Создать entity `DialogSession extends AuditableEntity` (пакет `model`)
- [x] Создать entity `DialogMessage extends AuditableEntity` (пакет `model`)
- [x] Создать `DialogSessionRepository` (пакет `repository`)
- [x] Создать `DialogMessageRepository` (пакет `repository`)

---

### 1.3 Хранилище памяти Spring AI

- [x] Реализовать `DbChatMemoryRepository implements ChatMemoryRepository` (пакет `service`) :
  - `add(conversationId, messages)` → сохраняет список `Message` в `dialog_messages`
  - `getMessages(conversationId, lastN)` → читает последние N сообщений сессии
  - `clear(conversationId)` → закрывает сессию (status = CLOSED)
- [x] В `ChatClientConfig` зарегистрировать `MessageWindowChatMemory` с `DbChatMemoryRepository`
- [x] Добавить `MessageChatMemoryAdvisor` в `ChatClient` bean

---

### 1.4 Tool загрузки субтитров

- [ ] Создать `SubtitlesLoaderTool` (пакет `tool`) — Spring-компонент с методом:
  ```java
  @Tool(description = "Загружает субтитры YouTube-видео по URL")
  public String loadSubtitles(String videoUrl)
  ```
  Внутри — текущая логика из `YtSubtitlesTool.loadSubtitles()`
- [ ] Зарегистрировать `SubtitlesLoaderTool` в `ChatClient` через `.defaultTools()`
- [ ] Удалить прямой вызов `YtSubtitlesTool` из `BotRetellingHandler`

---

### 1.5 Переработка AiClient

- [ ] Метод `startRetelling(String conversationId, String videoUrl)`:
  - Передаёт URL как user-сообщение в `chatClient` с `conversationId`
  - LLM сам вызывает tool, получает субтитры, формирует пересказ
  - Возвращает текст пересказа
- [ ] Метод `continueDialog(String conversationId, String userMessage)`:
  - Передаёт вопрос пользователя в `chatClient` с `conversationId`
  - Возвращает ответ LLM
- [ ] Оба метода пробрасывают исключение контекста наверх (не глотают)

---

### 1.6 Сервис управления сессиями

- [ ] Создать `DialogDomainService` (пакет `service/domain`):
  - `openSession(clientId, videoUrl)` → закрывает активную сессию (если есть), создаёт новую, возвращает `sessionId`
  - `findActiveSession(clientId)` → возвращает `Optional<DialogSession>`
  - `closeSession(sessionId)` → устанавливает `status = CLOSED`, `closed_at = now()`

---

### 1.7 Переработка BotRetellingHandler

- [ ] При получении YouTube-ссылки:
  1. Вызвать `DialogDomainService.openSession()` — получить `sessionId`
  2. Отправить сообщение «Извлекаю содержание...»
  3. Вызвать `AiClient.startRetelling(sessionId, url)`
  4. Отправить пересказ
  5. Отправить сообщение «Можете задавать вопросы по содержанию видео»
- [ ] При получении обычного сообщения (не ссылки):
  - Если активная сессия есть → `AiClient.continueDialog(sessionId, message)`
  - Если нет → «Пришлите ссылку на YouTube-видео»
- [ ] При поимке исключения превышения контекста от Spring AI:
  - `DialogDomainService.closeSession(sessionId)`
  - Отправить сообщение: «Объём диалога превысил 200 000 токенов — разговор завершён. Пришлите новую ссылку для продолжения»

---

### 1.8 Конфигурация

- [ ] Добавить в `application.yml` под `retelling`:
  ```yaml
  retelling:
    dialog-context-limit-tokens: 200000
  ```
- [ ] Добавить поле в `RetellingProperties` (или создать отдельный `@ConfigurationProperties`)

---

### 1.9 Тесты

- [ ] Unit-тест `DbChatMemoryRepositoryTest` — сохранение и чтение сообщений
- [ ] Unit-тест `DialogDomainServiceTest` — открытие/закрытие сессий
- [ ] Unit-тест `BotRetellingHandlerTest` — ветки: новая ссылка / вопрос с сессией / вопрос без сессии / превышение контекста