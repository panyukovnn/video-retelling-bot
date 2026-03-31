package ru.panyukovnn.videoretellingbot.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dialog_message")
public class DialogMessage extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    /**
     * Идентификатор сессии диалога
     */
    private UUID sessionId;
    /**
     * Роль отправителя сообщения
     */
    @Enumerated(EnumType.STRING)
    private MessageRole role;
    /**
     * Содержимое сообщения
     */
    private String content;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DialogMessage that = (DialogMessage) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}