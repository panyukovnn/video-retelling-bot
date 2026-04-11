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
@Table(name = "token_usage")
public class TokenUsage extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    /**
     * Идентификатор клиента, с которого произведено списание
     */
    private UUID clientId;
    /**
     * Идентификатор сессии диалога, если списание произошло в рамках диалога; иначе null
     */
    private UUID dialogSessionId;
    /**
     * Количество входных токенов, попавших в кэш DeepSeek
     */
    private Integer cacheHitInputTokens;
    /**
     * Количество входных токенов с промахом кэша DeepSeek
     */
    private Integer cacheMissInputTokens;
    /**
     * Количество выходных токенов, полученных от DeepSeek
     */
    private Integer outputTokens;
    /**
     * Итоговое количество списанных токенов с учётом весов приведения к output-эквиваленту
     */
    private Long chargedTokens;
    /**
     * Категория события списания
     */
    @Enumerated(EnumType.STRING)
    private TokenUsageKind kind;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenUsage that = (TokenUsage) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}