package ru.panyukovnn.videoretellingbot.model.content;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.panyukovnn.videoretellingbot.model.AuditableEntity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "contents")
public class Content extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Ссылка на материал
     */
    private String link;
    /**
     * Язык
     */
    @Enumerated(EnumType.STRING)
    private Lang lang;
    /**
     * Тип контента: статья, субтитры
     */
    @Enumerated(EnumType.STRING)
    private ContentType type;
    /**
     * Тип источника
     */
    @Enumerated(EnumType.STRING)
    private Source source;
    /**
     * Метаинформация: спецэффические подробности для каждого из источников
     */
    @JdbcTypeCode(SqlTypes.JSON)
    private String meta;
    /**
     * Заголовок
     */
    private String title;
    /**
     * Описание
     */
    private String description;
    /**
     * Дата публикации
     */
    private LocalDateTime publicationDate;
    /**
     * Содержимое источника
     */
    private String content;
    /**
     * Идентификатор группы контента
     */
    private UUID batchId;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Content content = (Content) o;
        return Objects.equals(id, content.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
