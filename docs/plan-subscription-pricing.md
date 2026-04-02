# Plan: Изменение модели оплаты — 100 звёзд за 50 видео

## 1. Конфигурация новой цены

- [x] В `application.yml` обновить/добавить свойства:
  ```yaml
  retelling:
    payment:
      stars-price: 100
      videos-per-purchase: 50
  ```
- [x] Создать или обновить `PaymentProperties` (`@ConfigurationProperties("retelling.payment")`) с полями `starsPrice` и `videosPerPurchase`

## 2. Обновление сущности клиента

- [x] Добавить в `Client` поле `paidRetellingsRemaining` (INT, DEFAULT 0) — количество оставшихся платных пересказов
- [x] Создать Liquibase миграцию `add_column_paid_retellings_remaining_to_clients.sql`

## 3. Логика начисления и списания

- [x] В `StarPaymentDomainService.confirmPayment()` после записи платежа увеличивать `client.paidRetellingsRemaining += videosPerPurchase`
- [x] В `AccessChecker.checkAccess()` при проверке платного доступа:
  - Если `paidRetellingsRemaining > 0` — доступ разрешён, уменьшать счётчик на 1 при каждом пересказе
  - Иначе — предлагать оплату
- [x] Убедиться, что списание `paidRetellingsRemaining` происходит только после успешного пересказа

## 4. Обновление инвойса

- [x] В `StarPaymentDomainService.sendInvoice()` обновить сумму на `starsPrice` из `PaymentProperties`
- [x] Обновить текст инвойса: указать, что покупается `videosPerPurchase` пересказов

## 5. Команда /status

- [x] В `StatusCommand` отображать `paidRetellingsRemaining` — сколько платных пересказов осталось

## 6. Тесты

- [x] `AccessCheckerTest`:
  - `when_checkAccess_withPaidRetellingsAvailable_then_allowedPaid`
  - `when_checkAccess_withNoPaidRetellings_then_requiresPayment`
- [x] `StarPaymentDomainServiceTest`:
  - `when_confirmPayment_then_paidRetellingsIncreasedByPackageSize`
- [x] `ClientDomainServiceTest`:
  - `when_decrementPaidRetellings_withZeroRemaining_then_exceptionThrown`
