package ru.panyukovnn.videoretellingbot.serivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.panyukovnn.videoretellingbot.property.PaymentProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMarkupChecker {

    private static final double TOKENS_PER_MILLION = 1_000_000.0;

    private final PaymentProperties paymentProperties;

    /**
     * Выполняет самопроверку тарифа на старте приложения.
     * Считает фактическую наценку продажи пакета токенов относительно себестоимости в DeepSeek
     * и логирует её INFO-сообщением, а при падении ниже {@code markupMinThreshold} — WARN,
     * чтобы быстро ловить рассинхрон цен DeepSeek и курса звезды Telegram.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkMarkupOnStartup() {
        double costInUsd = paymentProperties.getTokensPerPurchase()
            * paymentProperties.getDeepseekOutputPricePerMTokens()
            / TOKENS_PER_MILLION;
        double costInStars = costInUsd / paymentProperties.getStarUsdRate();
        double markup = paymentProperties.getStarsPrice() / costInStars;

        log.info("Тарифная самопроверка: пакет {} токенов продаётся за {} звёзд, себестоимость ≈ {} USD (≈ {} звёзд), фактическая наценка x{}",
                paymentProperties.getTokensPerPurchase(),
                paymentProperties.getStarsPrice(),
                String.format("%.4f", costInUsd),
                String.format("%.2f", costInStars),
                String.format("%.2f", markup));

        if (markup < paymentProperties.getMarkupMinThreshold()) {
            log.warn("Фактическая наценка x{} ниже порога x{} — проверьте актуальность цен DeepSeek (hit={}, miss={}, out={}) и курса звезды {}",
                    String.format("%.2f", markup),
                    String.format("%.2f", paymentProperties.getMarkupMinThreshold()),
                    paymentProperties.getDeepseekCacheHitPricePerMTokens(),
                    paymentProperties.getDeepseekCacheMissPricePerMTokens(),
                    paymentProperties.getDeepseekOutputPricePerMTokens(),
                    paymentProperties.getStarUsdRate());
        }
    }
}