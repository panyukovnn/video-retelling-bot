# План: переход на оплату по токенам

## Контекст

Текущая модель оплаты (см. `AccessChecker`, `StarPaymentDomainService`, `PaymentProperties`):

- 1 бесплатный пересказ в сутки — `clients.daily_retellings_used`, сброс по московскому времени.
- Покупка: 100 звёзд Telegram = пакет из 50 пересказов, остаток хранится в `clients.paid_retellings_remaining`.
- Списание — по одному пересказу, независимо от длины субтитров, длины ответа и числа уточняющих вопросов в диалоге.

Проблемы:

- Короткое 2-минутное видео тарифицируется наравне с часовым докладом.
- Диалог-уточнения после пересказа не учитывается в счёте вовсе.
- Пользователь не видит реального расхода — нет прозрачности.

DeepSeek в ответе возвращает `usage` с разбивкой на `prompt_cache_hit_tokens`, `prompt_cache_miss_tokens`, `completion_tokens`. Spring AI отдаёт это через `ChatResponse.getMetadata().getUsage()` (и native-metadata для cache-полей). Значит техническая база для учёта уже есть.

## Цель

Перейти на биллинг «по токенам»:

- Пользователь покупает фиксированный объём токенов за звёзды.
- Списывается ровно столько, сколько реально потратила модель, с учётом кэша.
- После каждого ответа и по `/status` пользователь видит понятную статистику расхода и остаток.

## Зафиксированные решения

- **Наценка ≈ x2.4** поверх себестоимости DeepSeek в звёздах Telegram (компромисс между «справедливо» и «дёшево выглядит для пользователя» — см. раздел «Тариф и формула»).
- **Единицей учёта** выступает «эквивалентный выходной токен» (output-equivalent): все типы токенов приводятся к output через коэффициенты (см. раздел «Тариф и формула»). Пользователю это подаётся как просто «токены» — один счётчик.
- **Бесплатного дневного лимита нет.** При первом взаимодействии (`/start` или первое сообщение) начисляется разовый **приветственный бонус 300 000 токенов**. Повторно не выдаётся.
- **Старые механики выпиливаются полностью:** `daily_retellings_used`, `daily_retellings_reset_date`, `paid_retellings_remaining`. Поля удаляем из кода, из таблицы — помечаем Deprecated и оставляем пустыми (без записи). Существующие пользователи при следующем обращении автоматически получают приветственный бонус (см. раздел «Миграция»).
- **Поведение при исчерпании баланса в середине ответа:** текущий стрим доводится до конца без ошибок, после финального чанка добавляется строка «Баланс токенов исчерпан — пополните, чтобы продолжить». Следующий запрос блокируется и показывается инвойс.
- **Статистика** показывается сразу после каждого ответа (и пересказа, и ответа в диалоге) отдельным коротким сообщением. В `/status` тоже дублируется текущий остаток и суммарные показатели.

## Тариф и формула

Цены DeepSeek (`deepseek-chat`, на момент принятия решения) — $ за 1 000 000 токенов:

| Тип токенов | Цена, $/1M | Вес (приведение к output) |
|---|---|---|
| cache hit input  | 0.07 | 0.064 |
| cache miss input | 0.27 | 0.245 |
| output           | 1.10 | 1.000 |

Формула списания за один ответ модели:

```
charged = round(cacheHit * 0.064 + cacheMiss * 0.245 + output * 1.000)
```

То есть внутренний счётчик — это «сколько стоил бы ответ, будь в нём только выходные токены». Это делает тариф справедливым: длинные ответы и промахи кэша стоят дороже, кэш-хиты — почти бесплатны.

Курс звёзд: 1 Star ≈ $0.013 (значение закладывается в `application.yaml`, чтобы быстро менять при корректировке цен Telegram).

Пакет и приветственный бонус:

- **1 пакет = 500 000 токенов за 100 звёзд** (проверка наценки):
    - себестоимость 500 000 output-эквивалентных токенов: `500000 * $1.10/1M = $0.55`
    - в звёздах: `$0.55 / $0.013 ≈ 42.3 stars`
    - цена продажи 100 stars → множитель ≈ **x2.36**
- **Приветственный бонус: 300 000 токенов** (одноразово при первом обращении клиента).

Все конкретные цифры (`tokensPerPurchase`, `starsPrice`, `welcomeBonusTokens`, цены DeepSeek, курс звезды, множитель наценки, веса) выносятся в `application.yaml` под `retelling.payment.*` — никаких magic-чисел в коде.

## Архитектурные принципы решения

- Списание — фактом, после получения `usage` из модели. Никаких предварительных оценок для списания.
- Предварительная проверка («хватит ли?») — эвристика по минимальному резерву, без фиктивного списания.
- Усреднение токенов: для отображения считаем фактические значения DeepSeek. Если модель не вернула usage — падаем на оценку через `TokenCountEstimator` (уже подключён в `ChatClientConfig`) и логируем warning.
- Никакого magic-number: все тарифы, коэффициенты, дневные лимиты и бонусы — в `application.yaml` под `retelling.payment.*`.
- Старые колонки таблицы `clients` (`daily_retellings_used`, `daily_retellings_reset_date`, `paid_retellings_remaining`) из структуры БД не удаляем (backward-compat по манифесту), но перестаём читать/писать их в коде.

## План реализации (checkbox)

### 1. Свойства и конфигурация

- [x] Расширить `PaymentProperties`:
    - `starsPrice` — цена одного пакета в звёздах (100)
    - `tokensPerPurchase` — сколько токенов даёт пакет (300 000)
    - `welcomeBonusTokens` — приветственный бонус (300 000)
    - `minTokenReserve` — минимальный остаток, при котором ещё разрешаем начать новый запрос к модели
    - `cachedInputWeight`, `inputWeight`, `outputWeight` — веса приведения к output-эквиваленту (0.064 / 0.245 / 1.0)
    - `deepseekCacheHitPricePerMTokens`, `deepseekCacheMissPricePerMTokens`, `deepseekOutputPricePerMTokens` — публичные цены DeepSeek, чтобы веса можно было пересчитать автоматически или проверить
    - `starUsdRate` — курс одной звезды в долларах (0.013)
    - `markupMinThreshold` — минимально допустимая наценка (напр., 1.5), ниже которой на старте приложения логируется WARN о рассинхроне цен
- [x] В `application.yaml` (и `application-localdev.yaml`) выставить значения под ключом `retelling.payment`.
- [x] На старте приложения делать самопроверку: рассчитывать фактическую наценку `stars / (tokens * outputPrice / starUsdRate)` и логировать её INFO-сообщением. Если наценка уходит ниже `markupMinThreshold` (напр., 1.5) — лог WARN с указанием актуальных цен DeepSeek, чтобы быстро ловить рассинхрон.
- [x] Включить у Spring AI OpenAI клиента `stream_options.include_usage=true`, чтобы usage приходил в последнем чанке стрима. Проверить через интеграционный прогон, что DeepSeek действительно отдаёт cache-метрики.

### 2. Доменная модель и БД

- [ ] В `Client` добавить поля:
    - `tokenBalance` (Long, NOT NULL, default 0)
    - `welcomeBonusGranted` (Boolean, NOT NULL, default false) — признак выданного приветственного бонуса
    - `totalTokensCharged` (Long) — сумма списанных (взвешенных) токенов за всё время
    - `totalCacheHitInputTokens` (Long)
    - `totalCacheMissInputTokens` (Long)
    - `totalOutputTokens` (Long)
    - `version` (Long, `@Version`) — оптимистическая блокировка при конкурентных списаниях
- [ ] В `Client` пометить Deprecated (в Javadoc) старые поля: `dailyRetellingsUsed`, `dailyRetellingsResetDate`, `paidRetellingsRemaining`. Геттеры остаются (для истории), в новом коде не используются.
- [ ] Новая entity `TokenUsage` (таблица `token_usage`):
    - `id` UUID
    - `client_id` UUID
    - `dialog_session_id` UUID (nullable)
    - `cache_hit_input_tokens` int
    - `cache_miss_input_tokens` int
    - `output_tokens` int
    - `charged_tokens` long — итого списано (с учётом весов)
    - `kind` enum (`RETELLING` / `DIALOG_QUESTION`)
    - `created_at` timestamp
- [ ] В `StarPayment` добавить поле `tokens_granted` (int) — фактически начисленный объём.
- [ ] Liquibase `v1.2.0`:
    - [ ] `01.alter_clients_add_token_balance.yml` — колонки `token_balance`, `welcome_bonus_granted`, агрегаты, `version`
    - [ ] `02.create_token_usage.yml` (+ индекс `idx_token_usage_client_id_created_at`)
    - [ ] `03.alter_star_payment_add_tokens_granted.yml`
- [ ] Repository `TokenUsageRepository`.

### 3. Захват usage из Spring AI

- [ ] В `AiClient` заменить `.stream().content()` на `.stream().chatResponse()`, чтобы на завершении стрима получить `ChatResponse` с `Usage` и native-metadata.
- [ ] Новый inner record `TokenUsageSnapshot(int cacheHitInput, int cacheMissInput, int output)` — публичный DTO для отдачи наверх.
- [ ] Метод `AiClient` возвращает не `Flux<String>`, а пару «стрим токенов + `Mono<TokenUsageSnapshot>`» (например, обёртка `RetellingStream` с полями `Flux<String> tokens` и `Mono<TokenUsageSnapshot> usage`).
- [ ] Юнит-тест на разбор `ChatResponse`:
    - есть cache-поля;
    - нет cache-полей — fallback на `promptTokens`;
    - нет usage — fallback на `TokenCountEstimator`.

### 4. Сервис биллинга

- [ ] Создать `TokenBillingService` (пакет `serivce.domain`, с `impl`-реализацией):
    - `boolean canStart(Client client)` — проверка `tokenBalance >= minTokenReserve`.
    - `long charge(Client client, TokenUsageSnapshot snapshot, TokenUsage.Kind kind, UUID sessionId)` — рассчитывает `charged` по формуле из раздела «Тариф и формула», сохраняет `TokenUsage`, обновляет агрегаты и остаток (допускается уход в минус на величину одного ответа — см. раздел 6), возвращает сколько было списано.
    - `void topUp(Client client, int tokens)` — пополнение при оплате.
    - `void grantWelcomeBonusIfNeeded(Client client)` — одноразовое начисление при `welcomeBonusGranted == false`.
    - `TokenSpendingSummary getSpendingSummary(Client client)` — для `/status` и блока статистики после ответов.
- [ ] Все формулы с весами и цены — через `PaymentProperties`, без magic-чисел.
- [ ] Обновление `Client` и запись `TokenUsage` — в одной транзакции (`@Transactional`).
- [ ] Конкурентные списания защищены `@Version` на `Client`. При `OptimisticLockException` — retry c перечитыванием.
- [ ] Юнит-тесты:
    - расчёт `charged` по формуле с разными сочетаниями hit/miss/output;
    - идемпотентность `grantWelcomeBonusIfNeeded` (повторный вызов ничего не делает);
    - идемпотентность `topUp` по `telegramChargeId` (на уровне `StarPaymentDomainService`, см. раздел 7);
    - уход в минус допустим ровно на один ответ, следующий `canStart` возвращает false;
    - конкурентные списания (интеграционный с реальной БД).

### 5. Изменения в `AccessChecker`

- [ ] `checkAccess` упрощается до:
    - admin → `ALLOWED_ADMIN`;
    - `tokenBillingService.canStart(client)` → `ALLOWED`;
    - иначе → `REQUIRES_PAYMENT`.
- [ ] Enum `AccessResult` схлопывается до `ALLOWED`, `ALLOWED_ADMIN`, `REQUIRES_PAYMENT` (прежние `ALLOWED_FREE`/`ALLOWED_PAID` удаляются).
- [ ] Методы `incrementDailyUsage`, `decrementPaidRetellings`, `resetDailyCounterIfNeeded` удалить.
- [ ] Юнит-тесты переписать под новый enum.

### 6. Интеграция в `BotRetellingHandler` и `ClientDomainService`

- [ ] В `ClientDomainService.save` при создании нового клиента — вызывать `tokenBillingService.grantWelcomeBonusIfNeeded(client)`, чтобы приветственный бонус 300 000 был начислен ровно один раз при первом появлении пользователя.
- [ ] В `handleNewVideo`:
    - до запроса — `tokenBillingService.canStart(client)`;
    - если нет — отправить обновлённое `MSG_REQUIRES_PAYMENT` + `sendInvoice`;
    - после завершения стрима (стрим доводится до конца всегда, даже если баланса не хватит — поведение Q5) — получить `TokenUsageSnapshot`, вызвать `charge`, отправить сообщение со статистикой.
    - если после списания `tokenBalance <= 0` — добавить отдельную строку «Баланс токенов исчерпан — пополните, чтобы продолжить» + `sendInvoice`.
- [ ] В `handleUserQuestion`:
    - до запроса — `canStart`; если нет — ответ «баланс исчерпан» + инвойс, в модель не ходим;
    - после ответа — `charge` с `kind = DIALOG_QUESTION`, блок статистики;
    - аналогично: если после этого ответа баланс ушёл в ноль — уведомление и инвойс, сессию диалога не закрываем принудительно, просто блокируем следующий ход.
- [ ] Обновить `MSG_REQUIRES_PAYMENT`, убрать упоминание «50 пересказов», ввести описание через `tokensPerPurchase`.
- [ ] Юнит-тесты через подмену `AiClient` и `TokenBillingService`:
    - успешный пересказ с достаточным балансом;
    - первый заход пользователя — списание из welcome-бонуса;
    - списание уводит в ноль — пользователь получает уведомление и инвойс, ответ модели не обрывается;
    - повторный запрос при нулевом балансе — модель не вызывается, сразу инвойс.

### 7. Инвойс и подтверждение платежа

- [ ] `StarPaymentDomainService.sendInvoice` — описание: `"Пакет из %d токенов для пересказа видео"` с форматированием через `NumberFormat` по локали.
- [ ] `confirmPayment` — вместо начисления видео вызывает `tokenBillingService.topUp(client, tokensPerPurchase)`, сохраняет `StarPayment.tokensGranted`.
- [ ] Идемпотентность по `telegramChargeId` — проверка уже есть, убедиться, что новая логика не ломает повторную доставку.
- [ ] Юнит-тесты на `confirmPayment` (успех, повторная доставка, параллельная доставка).

### 8. UX и статистика

- [ ] Шаблон сообщения после каждого ответа (пересказ или диалог):
    ```
    Расход: N токенов
      • кэш: a (вход из кэша)
      • вход: b
      • выход: c
    Остаток: M токенов
    ```
- [ ] Если после списания `M <= 0` — добавить строку «Баланс токенов исчерпан — пополните, чтобы продолжить», затем отправить инвойс.
- [ ] Обновить `StatusCommand`:
    - остаток токенов;
    - суммарный расход за всё время (hit / miss / output, charged);
    - приглашение к покупке: «Пакет 500 000 токенов за 100 звёзд Telegram»;
    - для администратора — неизменное «пересказы без ограничений».
- [ ] Обновить `StartCommand` — описать новую модель и сообщить о приветственных 300 000 токенов.
- [ ] Обновить `README.md` — раздел оплаты.
- [ ] Тесты на формирование сообщений `StatusCommand` и блока статистики.

### 9. Очистка старой модели (обратная совместимость БД)

- [ ] Поля `daily_retellings_used`, `daily_retellings_reset_date`, `paid_retellings_remaining` из таблицы `clients` НЕ удалять — согласно правилам манифеста о совместимости структуры БД.
- [ ] В `Client` пометить их Deprecated, в новом коде не читать и не писать.
- [ ] Удалить из `AccessChecker` всю старую логику (см. раздел 5).
- [ ] Удалить `MSG_REQUIRES_PAYMENT` с текстом про 50 пересказов, оставить только новый вариант.
- [ ] В `PaymentProperties` удалить `videosPerPurchase`.

### 10. Наблюдаемость

- [ ] Логировать в каждом списании: `clientId`, `kind`, `cacheHit`, `cacheMiss`, `output`, `charged`, `balanceAfter` — одним structured сообщением.
- [ ] При fallback (usage не пришёл) — `WARN` с chat-id и причиной.
- [ ] Админ-команда (опционально, обсуждается отдельно) — `/admin stats <userId>` для быстрой диагностики.

### 11. Тесты

- [ ] Unit: `TokenBillingService`, `AiClient` (usage parsing), `AccessChecker`, `BotRetellingHandler`, `StatusCommand`, `StarPaymentDomainService`, `ClientDomainService` (welcome bonus).
- [ ] Интеграционный тест `BotRetellingHandler` с подменой `AiClient` — полный путь: первый заход → приветственный бонус → пересказ → списание → статистика → исчерпание → инвойс.
- [ ] Liquibase-тест на пустой схеме (создание новых таблиц и колонок без падений).
- [ ] Smoke-тест: реальный вызов DeepSeek в dev-окружении, чтобы убедиться, что `stream_options.include_usage` возвращает кэш-метрики.

### 12. Документация и релиз

- [ ] Обновить `README.md` — новый раздел оплаты.
- [ ] Добавить в `docs/token-based-payment/` диаграмму потока (ASCII или mermaid): вход → проверка баланса → вызов модели → разбор usage → списание → ответ пользователю + блок статистики.
- [ ] Релиз тегом `v1.2.0`.

## Риски

- **DeepSeek и usage в стриме.** Надо убедиться, что при включённом `stream_options.include_usage` cache-поля действительно приходят. Если Spring AI не пробрасывает их через `Usage` — доставать из `ChatResponse.getMetadata().get("...")` или опуститься на `OpenAiApi` напрямую.
- **Гонки при параллельных списаниях.** Пользователь может параллельно запустить пересказ и задавать вопрос — защищаемся `@Version` + retry.
- **Неточная оценка «хватит ли на пересказ».** У разных видео разная длина субтитров. Используем `minTokenReserve` как порог; после первого же пересказа, уведшего в ноль, следующие запросы блокируются.
- **Цены DeepSeek могут меняться.** Веса и сколько токенов за пакет — параметры, а не код; при изменении цены достаточно обновить yaml. Самопроверка наценки x4 на старте приложения предупредит о рассинхроне.
- **Существующие пользователи с ненулевым `paid_retellings_remaining`** на момент релиза теряют этот остаток (мы выпиливаем старую модель). Вместо этого они получают приветственный бонус 300 000 токенов при следующем обращении. Это сознательный компромисс — вариант согласован.

## Порядок работ

1. Принять итоговые числа (ниже). Если всё ок — приступаем.
2. Реализация по разделам 1 → 12 в порядке перечисления.
3. Smoke-прогон на dev с реальным DeepSeek.
4. Релиз и мониторинг логов списания первые сутки.

## Итоговые числа (нуждаются в финальном подтверждении)

| Параметр | Значение |
|---|---|
| `starsPrice` | 100 |
| `tokensPerPurchase` | 500 000 |
| `welcomeBonusTokens` | 300 000 |
| `minTokenReserve` | 0 (стрим доводится даже при нуле, блокируется только следующий запрос) |
| `cachedInputWeight` | 0.064 |
| `inputWeight` | 0.245 |
| `outputWeight` | 1.000 |
| `markupMinThreshold` (для самопроверки на старте) | 1.5 |
| `starUsdRate` | 0.013 |
| `deepseekCacheHitPricePerMTokens` | 0.07 |
| `deepseekCacheMissPricePerMTokens` | 0.27 |
| `deepseekOutputPricePerMTokens` | 1.10 |
| Эффективная наценка при текущих числах | ≈ x2.36 |