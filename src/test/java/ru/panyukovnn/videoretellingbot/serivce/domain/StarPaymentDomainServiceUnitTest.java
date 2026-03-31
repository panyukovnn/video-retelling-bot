package ru.panyukovnn.videoretellingbot.serivce.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import ru.panyukovnn.longpollingtgbotstarter.config.TgBotApi;
import ru.panyukovnn.videoretellingbot.model.Client;
import ru.panyukovnn.videoretellingbot.model.StarPayment;
import ru.panyukovnn.videoretellingbot.repository.StarPaymentRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StarPaymentDomainServiceUnitTest {

    private final TgBotApi tgBotApi = mock(TgBotApi.class);
    private final StarPaymentRepository starPaymentRepository = mock(StarPaymentRepository.class);

    private final StarPaymentDomainService service = new StarPaymentDomainService(tgBotApi, starPaymentRepository);

    @Nested
    class SendInvoiceTest {

        @Test
        void when_sendInvoice_then_executesWithCorrectParams() throws Exception {
            long chatId = 100L;
            String videoUrl = "https://www.youtube.com/watch?v=abc";

            service.sendInvoice(chatId, videoUrl);

            ArgumentCaptor<SendInvoice> captor = ArgumentCaptor.forClass(SendInvoice.class);
            verify(tgBotApi).execute(captor.capture());

            SendInvoice invoice = captor.getValue();
            assertEquals(String.valueOf(chatId), invoice.getChatId());
            assertEquals("XTR", invoice.getCurrency());
            assertEquals(videoUrl, invoice.getPayload());
            assertEquals("", invoice.getProviderToken());
            assertEquals(1, invoice.getPrices().size());
            assertEquals(1, invoice.getPrices().get(0).getAmount());
        }

        @Test
        void when_sendInvoice_andExecuteFails_then_throwsException() throws Exception {
            when(tgBotApi.execute(any(SendInvoice.class))).thenThrow(new RuntimeException("API error"));

            assertThrows(RuntimeException.class, () -> service.sendInvoice(100L, "https://youtube.com/watch?v=abc"));
        }
    }

    @Nested
    class ConfirmPayment {

        @Test
        void when_confirmPayment_then_savesStarPayment() {
            UUID clientId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).tgUserId(100L).build();
            String chargeId = "charge_123";
            String videoUrl = "https://www.youtube.com/watch?v=abc";

            service.confirmPayment(client, chargeId, videoUrl);

            ArgumentCaptor<StarPayment> captor = ArgumentCaptor.forClass(StarPayment.class);
            verify(starPaymentRepository).save(captor.capture());

            StarPayment saved = captor.getValue();
            assertEquals(client, saved.getClient());
            assertEquals(chargeId, saved.getTelegramChargeId());
            assertEquals(videoUrl, saved.getVideoUrl());
        }
    }
}