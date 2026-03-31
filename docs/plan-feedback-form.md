# Plan: Периодическая ссылка на форму обратной связи

## 1. Конфигурация

- [ ] В `application.yml` добавить новую группу свойств:
  ```yaml
  retelling:
    feedback:
      form-url: "https://..."        # URL Google Forms
      show-every-n-retellings: 5     # каждые N пересказов показывать ссылку
  ```
- [ ] Создать `FeedbackProperties` (`@ConfigurationProperties("retelling.feedback")`) с полями `formUrl` и `showEveryNRetellings`

## 2. Логика показа ссылки

- [ ] В `ClientDomainService.incrementRetellingsCount()` (или в `BotRetellingHandler` после успешного пересказа) проверять: если `client.retellingsCount % showEveryNRetellings == 0` — добавить ссылку в ответ
- [ ] Создать вспомогательный метод `buildFeedbackMessage(String formUrl): String`, возвращающий готовый текст сообщения вида:
  ```
  Если у вас есть пожелания или жалобы — оставьте отзыв: <ссылка>
  ```
- [ ] Добавить константу шаблона сообщения в `Constants.java`

## 3. Отправка сообщения

- [ ] Отправлять сообщение с формой как отдельное сообщение после ответа бота (не склеивать с пересказом)

## 4. Тесты

- [ ] `FeedbackMessageSenderTest` (или в рамках `BotRetellingHandlerTest`):
  - `when_handleNewVideo_withRetellingsCountDivisibleByN_then_feedbackMessageSent`
  - `when_handleNewVideo_withRetellingsCountNotDivisible_then_feedbackMessageNotSent`
