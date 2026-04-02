package ru.panyukovnn.videoretellingbot.serivce.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.panyukovnn.videoretellingbot.model.Client;
import ru.panyukovnn.videoretellingbot.model.StarPayment;
import ru.panyukovnn.videoretellingbot.property.PaymentProperties;
import ru.panyukovnn.videoretellingbot.repository.ClientRepository;
import ru.panyukovnn.videoretellingbot.repository.StarPaymentRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class StarPaymentDomainService {

    private static final String INVOICE_TITLE = "Пакет пересказов видео";
    private static final String INVOICE_DESCRIPTION_TEMPLATE = "Оплата %d дополнительных пересказов видео";
    private static final String CURRENCY_XTR = "XTR";

    private final TelegramClient telegramClient;
    private final PaymentProperties paymentProperties;
    private final ClientRepository clientRepository;
    private final StarPaymentRepository starPaymentRepository;

    /**
     * Отправляет инвойс на оплату пересказа через Telegram Stars
     */
    public void sendInvoice(long chatId, String videoUrl) {
        String description = String.format(
            INVOICE_DESCRIPTION_TEMPLATE, paymentProperties.getVideosPerPurchase()
        );

        SendInvoice sendInvoice = SendInvoice.builder()
            .chatId(chatId)
            .title(INVOICE_TITLE)
            .description(description)
            .payload(videoUrl)
            .providerToken("")
            .currency(CURRENCY_XTR)
            .price(new LabeledPrice(INVOICE_TITLE, paymentProperties.getStarsPrice()))
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
     * Подтверждает платёж, сохраняет запись и начисляет пакет оплаченных пересказов
     */
    public void confirmPayment(Client client, String chargeId, String videoUrl) {
        StarPayment payment = StarPayment.builder()
            .clientId(client.getId())
            .telegramChargeId(chargeId)
            .videoUrl(videoUrl)
            .build();

        starPaymentRepository.save(payment);

        int currentRemaining = client.getPaidRetellingsRemaining() == null
            ? 0 : client.getPaidRetellingsRemaining();
        client.setPaidRetellingsRemaining(currentRemaining + paymentProperties.getVideosPerPurchase());
        clientRepository.save(client);

        log.info("Платёж подтверждён, chargeId: {}, clientId: {}, начислено {} пересказов",
            chargeId, client.getId(), paymentProperties.getVideosPerPurchase());
    }
}