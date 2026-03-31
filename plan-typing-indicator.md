л # Plan: Индикатор ожидания ответа бота

## 1. Отправка «typing...» через Telegram API

- [x] Изучить метод `sendChatAction` в используемом starter'е `long-polling-tg-bot-starter`
- [x] В `TgBotListener` (или `BotRetellingHandler`) перед началом обработки нового видео и перед ответом на вопрос вызывать `sendChatAction(chatId, "typing")`
- [x] Если starter не поддерживает `sendChatAction` — добавить вызов через HTTP напрямую к Telegram Bot API в `TgBotListener`

## 2. Периодическое обновление статуса typing

- [x] `sendChatAction` действует 5 секунд; для длительных операций запускать повторную отправку каждые 4 секунды через `ScheduledExecutorService`
- [x] Остановить повторную отправку после получения ответа от AI или при ошибке

## 3. Сообщение о начале обработки

- [x] При получении новой YouTube-ссылки до начала загрузки субтитров отправлять пользователю сообщение вида:
  ```
  Загружаю и обрабатываю видео, подождите...
  ```
- [x] Добавить константу `PROCESSING_MESSAGE` в `Constants.java`

## 4. Тесты

- [x] `BotRetellingHandlerTest`:
  - `when_handleNewVideo_then_processingMessageSent`
  - `when_handleNewVideo_then_typingActionSent`