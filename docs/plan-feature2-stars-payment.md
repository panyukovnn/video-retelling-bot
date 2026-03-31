# Feature 2: Оплата пересказов через Telegram Stars

## Описание

Модель оплаты: **1 звезда = 1 пересказ** (pay-per-use).

- 1 пересказ в день — бесплатно
- Каждый дополнительный пересказ — 1 Telegram Star (валюта `XTR`)
- Администраторы из конфига пользуются ботом бесплатно без ограничений

### Как работают Telegram Stars

1. Бот вызывает `sendInvoice` с параметрами:
   - `currency = "XTR"`
   - `provider_token = ""` (пустая строка — обязательно для Stars)
   - `prices = [LabeledPrice("Пересказ видео", 1)]` (1 звезда)
   - `payload` — произвольная строка для идентификации платежа на стороне бота (например, `userId:videoUrl`)
2. Пользователь видит инвойс в чате и нажимает «Оплатить»
3. Telegram присылает боту `pre_checkout_query` — бот **обязан** ответить `answerPreCheckoutQuery(ok=true)` в течение 10 секунд, иначе оплата отменяется
4. После успешной оплаты Telegram присылает `successful_payment` с `telegramPaymentChargeId` — уникальным ID транзакции
5. Бот сохраняет запись об оплате и выполняет пересказ

---

## Чеклист реализации

### 2.1 Миграции БД

- [x] Добавить в миграцию `v1.1.0/changelog.yml` (вместе с Feature 1):
  - В таблицу `clients` добавить поля:
    - `daily_retellings_used` INT DEFAULT 0
    - `daily_retellings_reset_date` DATE NULLABLE
- [x] Таблица `star_payments`:
- `id` UUID PK
  - `client_id` UUID FK → `clients.id`
  - `telegram_charge_id` VARCHAR UNIQUE — ID транзакции от Telegram
  - `video_url` VARCHAR — за какое видео оплачено
  - аудит: `create_time`, `create_user`, `last_update_time`, `last_update_user` (как в `clients`)

---

### 2.2 JPA-модели и репозитории

- [x] Добавить поля `dailyRetellingsUsed` и `dailyRetellingsResetDate` в entity `Client`
- [x] Создать entity `StarPayment extends AuditableEntity` (пакет `model`)
- [x] Создать `StarPaymentRepository` (пакет `repository`) с методом `existsByTelegramChargeId()`

---

### 2.3 Конфигурация администраторов

- [x] Создать `AdminProperties` (`@ConfigurationProperties("retelling.admin")`, пакет `property`):
  ```java
  @ConfigurationProperties("retelling.admin")
  public class AdminProperties {
      private List<Long> userIds = List.of();
  }
  ```
- [x] Добавить в `application.yml`:
  ```yaml
  retelling:
    admin:
      user-ids: []
  ```

---

### 2.4 Сервис проверки лимитов

- [x] Создать `AccessChecker` (пакет `service`):
  - `checkAccess(Client client)` — возвращает `AccessResult`: `ALLOWED_FREE`, `ALLOWED_ADMIN`, `REQUIRES_PAYMENT`
  - Логика:
    1. `userId` в `AdminProperties.userIds` → `ALLOWED_ADMIN`
    2. `daily_retellings_reset_date != today` → сбросить счётчик (`used = 0`, дату обновить)
    3. `daily_retellings_used == 0` → `ALLOWED_FREE` (счётчик инкрементировать после успешного пересказа)
    4. Иначе → `REQUIRES_PAYMENT`
- [x] Создать внутренний enum `AccessResult` в `AccessChecker`

---

### 2.5 Сервис платежей

- [ ] Создать `StarPaymentDomainService` (пакет `service/domain`):
  - `sendInvoice(long chatId, String videoUrl)` — вызывает `TgSender.sendInvoice()` с `currency="XTR"`, 1 звезда, `payload = videoUrl`
  - `confirmPayment(long userId, String chargeId, String videoUrl)` — сохраняет `StarPayment`, помечает что пересказ оплачен
  - `hasPendingPayment(long userId, String videoUrl)` — проверяет, есть ли неиспользованная оплата для данного URL

---

### 2.6 Обработка Telegram-событий оплаты

- [ ] В `TgBotListener` добавить обработку новых типов Update:
  - `pre_checkout_query`:
    - Вызвать `answerPreCheckoutQuery(queryId, ok=true)` немедленно
    - Логировать факт получения
  - `successful_payment`:
    - Вызвать `StarPaymentDomainService.confirmPayment(userId, chargeId, payload)`
    - Запустить пересказ через `BotRetellingHandler` (payload содержит videoUrl)
    - Отправить сообщение: «Оплата прошла успешно, начинаю пересказ!»

---

### 2.7 Интеграция с BotRetellingHandler

- [ ] Перед стартом пересказа:
  1. Получить `Client` из БД
  2. Вызвать `AccessChecker.checkAccess(client)`
  3. Если `ALLOWED_FREE` или `ALLOWED_ADMIN` → продолжить, после успешного пересказа инкрементировать `dailyRetellingsUsed`
  4. Если `REQUIRES_PAYMENT` → вызвать `StarPaymentDomainService.sendInvoice()`, прервать текущий флоу
- [ ] После успешного пересказа (FREE):
  - Инкрементировать `client.dailyRetellingsUsed`
  - Сохранить через `ClientDomainService`

---

### 2.8 Команды бота

- [ ] Команда `/status` в `StartCommand` или новый `StatusCommand`:
  - Администратор: «Вы администратор — пересказы бесплатны без ограничений»
  - Обычный пользователь: «Бесплатный пересказ на сегодня: использован / доступен. Стоимость дополнительного пересказа — 1 звезда»

---

### 2.9 UX при исчерпании лимита

- [ ] Текст при `REQUIRES_PAYMENT`:
  «Бесплатный пересказ на сегодня уже использован. Стоимость одного дополнительного пересказа — 1 звезда Telegram»
  → далее отправляется инвойс

---

### 2.10 Тесты

- [ ] Unit-тест `AccessCheckerTest`:
  - Администратор → всегда `ALLOWED_ADMIN`
  - Первый пересказ сегодня → `ALLOWED_FREE`
  - Второй пересказ сегодня → `REQUIRES_PAYMENT`
  - Первый пересказ после смены дня → `ALLOWED_FREE` (счётчик сброшен)
- [ ] Unit-тест `StarPaymentDomainServiceTest` — сохранение и поиск платежей
- [ ] Unit-тест `BotRetellingHandlerTest` — ветки с проверкой доступа