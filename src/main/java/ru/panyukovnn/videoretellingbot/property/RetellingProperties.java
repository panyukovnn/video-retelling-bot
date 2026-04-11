package ru.panyukovnn.videoretellingbot.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "retelling")
public class RetellingProperties {

    /**
     * Полный контекст модели (вход + выход), за вычетом которого рассчитывается бюджет токенов на входные сообщения
     */
    private int dialogContextLimitTokens;
    /**
     * Запас токенов на расхождения между локальным токенизатором и токенизатором модели, системные служебные токены
     */
    private int tokenSafetyMargin;
}
