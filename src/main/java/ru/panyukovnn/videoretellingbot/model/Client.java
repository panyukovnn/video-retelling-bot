package ru.panyukovnn.videoretellingbot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clients")
public class Client extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    /**
     * Идентификатор пользователя в Telegram
     */
    private Long tgUserId;
    /**
     * Идентификатор последнего чата с пользователем
     */
    private Long tgLastChatId;
    /**
     * Логин пользователя в Telegram
     */
    private String username;
    /**
     * Имя пользователя
     */
    private String firstname;
    /**
     * Фамилия пользователя
     */
    private String lastname;
    /**
     * Общее количество запросов на пересказ
     */
    private Long retellingsCount;
    /**
     * Количество использованных бесплатных пересказов за текущий день
     *
     * @deprecated поле старой модели оплаты, сохранено для обратной совместимости со структурой БД
     */
    @Deprecated
    private Integer dailyRetellingsUsed;
    /**
     * Дата последнего сброса счётчика бесплатных пересказов
     *
     * @deprecated поле старой модели оплаты, сохранено для обратной совместимости со структурой БД
     */
    @Deprecated
    private Instant dailyRetellingsResetDate;
    /**
     * Количество оставшихся оплаченных пересказов
     *
     * @deprecated поле старой модели оплаты, сохранено для обратной совместимости со структурой БД
     */
    @Deprecated
    private Integer paidRetellingsRemaining;
    /**
     * Текущий остаток токенов на балансе клиента
     */
    private Long tokenBalance;
    /**
     * Признак того, что клиенту уже был начислен приветственный бонус токенов
     */
    private Boolean welcomeBonusGranted;
    /**
     * Суммарно списано с клиента токенов (с учётом весов приведения к output-эквиваленту)
     */
    private Long totalTokensCharged;
    /**
     * Суммарное количество входных токенов клиента, попавших в кэш DeepSeek
     */
    private Long totalCacheHitInputTokens;
    /**
     * Суммарное количество входных токенов клиента с промахом кэша DeepSeek
     */
    private Long totalCacheMissInputTokens;
    /**
     * Суммарное количество выходных токенов клиента, полученных от DeepSeek
     */
    private Long totalOutputTokens;
    /**
     * Поле оптимистической блокировки для защиты от гонок при параллельных списаниях
     */
    @Version
    private Long version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Client client = (Client) o;

        return Objects.equals(id, client.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}