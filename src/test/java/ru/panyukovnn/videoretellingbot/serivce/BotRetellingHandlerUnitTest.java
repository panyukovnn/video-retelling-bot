package ru.panyukovnn.videoretellingbot.serivce;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.panyukovnn.longpollingtgbotstarter.service.TgSender;
import ru.panyukovnn.videoretellingbot.client.AiClient;
import ru.panyukovnn.videoretellingbot.model.Client;
import ru.panyukovnn.videoretellingbot.model.DialogSession;
import ru.panyukovnn.videoretellingbot.serivce.domain.DialogDomainService;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotRetellingHandlerUnitTest {

    private final TgSender tgSender = mock(TgSender.class);
    private final AiClient aiClient = mock(AiClient.class);
    private final DialogDomainService dialogDomainService = mock(DialogDomainService.class);

    private final BotRetellingHandler handler = new BotRetellingHandler(tgSender, aiClient, dialogDomainService);

    @Nested
    class HandleRetelling {

        @Test
        void when_handleRetelling_withYoutubeUrl_then_opensSessionAndSendsRetelling() {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            String videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";

            when(dialogDomainService.openSession(client, videoUrl)).thenReturn(sessionId);
            when(aiClient.startRetelling(sessionId.toString(), videoUrl)).thenReturn("Retelling text");

            handler.handleRetelling(chatId, client, videoUrl);

            verify(tgSender).send(chatId, "Извлекаю содержание...");
            verify(tgSender).send(chatId, "Retelling text");
            verify(tgSender).send(chatId, "Можете задавать вопросы по содержанию видео");
        }

        @Test
        void when_handleRetelling_withRegularMessageAndActiveSession_then_continuesDialog() {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            DialogSession activeSession = DialogSession.builder().id(sessionId).build();

            when(dialogDomainService.findActiveSession(clientId)).thenReturn(Optional.of(activeSession));
            when(aiClient.continueDialog(sessionId.toString(), "What is the topic?"))
                .thenReturn("The topic is...");

            handler.handleRetelling(chatId, client, "What is the topic?");

            verify(tgSender).send(chatId, "The topic is...");
        }

        @Test
        void when_handleRetelling_withRegularMessageAndNoSession_then_sendsLinkPrompt() {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();

            when(dialogDomainService.findActiveSession(clientId)).thenReturn(Optional.empty());

            handler.handleRetelling(chatId, client, "What is the topic?");

            verify(tgSender).send(chatId, "Пришлите ссылку на YouTube-видео");
            verifyNoInteractions(aiClient);
        }

        @Test
        void when_handleRetelling_withContextExceeded_then_closesSessionAndNotifiesUser() {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            DialogSession activeSession = DialogSession.builder().id(sessionId).build();

            when(dialogDomainService.findActiveSession(clientId)).thenReturn(Optional.of(activeSession));
            when(aiClient.continueDialog(sessionId.toString(), "Next question?"))
                .thenThrow(new RuntimeException("Context limit exceeded"));

            handler.handleRetelling(chatId, client, "Next question?");

            verify(dialogDomainService).closeSession(sessionId);
            verify(tgSender).send(chatId,
                "Объём диалога превысил 200 000 токенов — разговор завершён. Пришлите новую ссылку для продолжения"
            );
        }
    }
}
