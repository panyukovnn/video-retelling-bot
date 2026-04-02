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
     */
    private Integer dailyRetellingsUsed;
    /**
     * Дата последнего сброса счётчика бесплатных пересказов
     */
    private Instant dailyRetellingsResetDate;
    /**
     * Количество оставшихся оплаченных пересказов
     */
    private Integer paidRetellingsRemaining;

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