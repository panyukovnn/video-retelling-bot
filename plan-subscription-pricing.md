# Plan: Изменение модели оплаты — 100 звёзд за 50 видео

## 1. Конфигурация новой цены

- [ ] В `application.yml` обновить/добавить свойства:
  ```yaml
  retelling:
    payment:
      stars-price: 100
      videos-per-purchase: 50
  ```
- [ ] Создать или обновить `PaymentProperties` (`@ConfigurationProperties("retelling.payment")`) с полями `starsPrice` и `videosPerPurchase`

## 2. Обновление сущности клиента

- [ ] Добавить в `Client` поле `paidRetellingsRemaining` (INT, DEFAULT 0) — количество оставшихся платных пересказов
- [ ] Создать Liquibase миграцию `add_column_paid_retellings_remaining_to_clients.sql`

## 3. Логика начисления и списания

- [ ] В `StarPaymentDomainService.confirmPayment()` после записи платежа увеличивать `client.paidRetellingsRemaining += videosPerPurchase`
- [ ] В `AccessChecker.checkAccess()` при проверке платного доступа:
  - Если `paidRetellingsRemaining > 0` — доступ разрешён, уменьшать счётчик на 1 при каждом пересказе
  - Иначе — предлагать оплату
- [ ] Убедиться, что списание `paidRetellingsRemaining` происходит только после успешного пересказа

## 4. Обновление инвойса

- [ ] В `StarPaymentDomainService.sendInvoice()` обновить сумму на `starsPrice` из `PaymentProperties`
- [ ] Обновить текст инвойса: указать, что покупается `videosPerPurchase` пересказов

## 5. Команда /status

- [ ] В `StatusCommand` отображать `paidRetellingsRemaining` — сколько платных пересказов осталось

## 6. Тесты

- [ ] `AccessCheckerTest`:
  - `when_checkAccess_withPaidRetellingsAvailable_then_allowedPaid`
  - `when_checkAccess_withNoPaidRetellings_then_requiresPayment`
- [ ] `StarPaymentDomainServiceTest`:
  - `when_confirmPayment_then_paidRetellingsIncreasedByPackageSize`
- [ ] `ClientDomainServiceTest`:
  - `when_decrementPaidRetellings_withZeroRemaining_then_exceptionThrown`
