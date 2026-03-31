package ru.panyukovnn.videoretellingbot.serivce.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.panyukovnn.videoretellingbot.model.Client;
import ru.panyukovnn.videoretellingbot.model.StarPayment;
import ru.panyukovnn.videoretellingbot.repository.StarPaymentRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StarPaymentDomainService {

    private static final String INVOICE_TITLE = "Пересказ видео";
    private static final String INVOICE_DESCRIPTION = "Оплата одного дополнительного пересказа видео";
    private static final String CURRENCY_XTR = "XTR";
    private static final int STAR_PRICE = 1;

    private final TelegramClient telegramClient;
    private final StarPaymentRepository starPaymentRepository;

    /**
     * Отправляет инвойс на оплату пересказа через Telegram Stars
     */
    public void sendInvoice(long chatId, String videoUrl) {
        SendInvoice sendInvoice = SendInvoice.builder()
            .chatId(chatId)
            .title(INVOICE_TITLE)
            .description(INVOICE_DESCRIPTION)
            .payload(videoUrl)
            .providerToken("")
            .currency(CURRENCY_XTR)
            .price(new LabeledPrice(INVOICE_TITLE, STAR_PRICE))
            .build();

        try {
            telegramClient.execute(sendInvoice);
            log.info("Инвойс на оплату пересказа отправлен в чат {}", chatId);
        } catch (Exception e) {
            log.error("Ошибка при отправке инвойса в чат {}", chatId, e);

            throw new RuntimeException("Не удалось отправить инвойс для оплаты", e);
        }
    }

    /**
     * Подтверждает платёж и сохраняет запись о нём
     */
    public void confirmPayment(Client client, String chargeId, String videoUrl) {
        StarPayment payment = StarPayment.builder()
            .clientId(client.getId())
            .telegramChargeId(chargeId)
            .videoUrl(videoUrl)
            .build();

        starPaymentRepository.save(payment);
        log.info("Платёж подтверждён, chargeId: {}, clientId: {}", chargeId, client.getId());
    }
}