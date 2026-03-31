# Plan: Вынесение подписки в отдельный стартер

## 1. Создание нового модуля-стартера

- [ ] Создать новый Maven/Gradle модуль `subscription-starter` в отдельном репозитории или как подмодуль
- [ ] Настроить `build.gradle` / `pom.xml`: зависимости `spring-boot-autoconfigure`, `spring-data-jpa`, Lombok
- [ ] Создать `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` для автоконфигурации

## 2. Перенос сущностей и репозиториев подписки

- [ ] Перенести сущность `StarPayment` в стартер (пакет `ru.panyukovnn.subscriptionstarter.model`)
- [ ] Перенести `StarPaymentRepository` в стартер
- [ ] Перенести `StarPaymentDomainService` и его интерфейс в стартер

## 3. Перенос логики доступа

- [ ] Перенести `AccessChecker` в стартер
- [ ] Перенести `AccessResult` (enum или record с результатом проверки) в стартер
- [ ] Сделать поведение конфигурируемым через `@ConfigurationProperties`:
  ```yaml
  subscription:
    daily-free-retellings: 1
    admin-user-ids: []
  ```

## 4. Liquibase миграции в стартере

- [ ] Перенести миграции таблицы `star_payment` в стартер: `src/main/resources/db/changelog/subscription/`
- [ ] Добавить мастер-чейнджлог стартера, который подключается из потребляющего приложения

## 5. Автоконфигурация

- [ ] Создать `SubscriptionAutoConfiguration` — регистрирует все бины стартера
- [ ] Добавить условие `@ConditionalOnProperty("subscription.enabled")` для возможности отключения

## 6. Обновление основного приложения

- [ ] Добавить зависимость на `subscription-starter` в `build.gradle` основного приложения
- [ ] Удалить из основного приложения перенесённые классы
- [ ] Проверить, что `AccessChecker`, `StarPaymentDomainService` подхватываются через автоконфигурацию

## 7. Тесты

- [ ] В стартере: `AccessCheckerTest`:
  - `when_checkAccess_withAdminUser_then_allowedAdmin`
  - `when_checkAccess_withFreeQuotaAvailable_then_allowedFree`
  - `when_checkAccess_withExhaustedQuota_then_requiresPayment`
- [ ] Интеграционный тест автоконфигурации стартера: `SubscriptionAutoConfigurationTest`
