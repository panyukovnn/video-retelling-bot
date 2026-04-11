package ru.panyukovnn.videoretellingbot.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "retelling.payment")
public class PaymentProperties {

    /**
     * Цена одного пакета в звёздах Telegram
     */
    private int starsPrice;
    /**
     * Устаревшее поле — количество пересказов в одном пакете
     *
     * @deprecated используется только для обратной совместимости до удаления старой модели оплаты
     */
    @Deprecated
    private int videosPerPurchase;
    /**
     * Сколько токенов начисляется клиенту за один купленный пакет
     */
    private long tokensPerPurchase;
    /**
     * Приветственный бонус токенов, который выдаётся новому клиенту один раз при первом обращении
     */
    private long welcomeBonusTokens;
    /**
     * Минимальный остаток токенов, при котором ещё разрешается начать новый запрос к модели
     */
    private long minTokenReserve;
    /**
     * Вес приведения токенов, попавших в кэш ввода, к output-эквивалентным токенам
     */
    private double cachedInputWeight;
    /**
     * Вес приведения промахнувшихся мимо кэша входных токенов к output-эквивалентным токенам
     */
    private double inputWeight;
    /**
     * Вес приведения выходных токенов к output-эквивалентным токенам
     */
    private double outputWeight;
    /**
     * Публичная цена DeepSeek за 1 000 000 токенов, попавших в кэш ввода, в долларах
     */
    private double deepseekCacheHitPricePerMTokens;
    /**
     * Публичная цена DeepSeek за 1 000 000 входных токенов с промахом кэша в долларах
     */
    private double deepseekCacheMissPricePerMTokens;
    /**
     * Публичная цена DeepSeek за 1 000 000 выходных токенов в долларах
     */
    private double deepseekOutputPricePerMTokens;
    /**
     * Курс одной звезды Telegram в долларах
     */
    private double starUsdRate;
    /**
     * Минимально допустимая наценка — при значении ниже на старте приложения логируется WARN
     */
    private double markupMinThreshold;
}