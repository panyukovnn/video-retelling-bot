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
@Table(name = "dialog_session")
public class DialogSession extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    /**
     * Идентификатор клиента
     */
    private UUID clientId;
    /**
     * URL видео для пересказа
     */
    private String videoUrl;
    /**
     * Статус сессии
     */
    @Enumerated(EnumType.STRING)
    private DialogSessionStatus status;
    /**
     * Время закрытия сессии
     */
    private Instant closedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DialogSession that = (DialogSession) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}