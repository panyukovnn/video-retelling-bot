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
@Table(name = "publishing_channels")
public class PublishingChannel extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Имя (для читабельности)
     */
    private String name;
    /**
     * Строковый идентификатор (для удобства поиска)
     */
    private String externalId;
    /**
     * Идентификатор чата в телеграм
     */
    private Long chatId;
    /**
     * Идентификатор топика в телеграм
     */
    private Long topicId;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PublishingChannel content = (PublishingChannel) o;
        return Objects.equals(id, content.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
