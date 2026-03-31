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
import ru.panyukovnn.videoretellingbot.serivce.domain.StarPaymentDomainService;
import ru.panyukovnn.videoretellingbot.tool.YtSubtitlesTool;
import ru.panyukovnn.videoretellingbot.util.Constants;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotRetellingHandlerUnitTest {

    private final TgSender tgSender = mock(TgSender.class);
    private final AiClient aiClient = mock(AiClient.class);
    private final AccessChecker accessChecker = mock(AccessChecker.class);
    private final YtSubtitlesTool ytSubtitlesTool = mock(YtSubtitlesTool.class);
    private final DialogDomainService dialogDomainService = mock(DialogDomainService.class);
    private final StarPaymentDomainService starPaymentDomainService = mock(StarPaymentDomainService.class);
    private final TypingIndicator typingIndicator = mock(TypingIndicator.class);

    private final BotRetellingHandler handler = new BotRetellingHandler(
        tgSender, aiClient, accessChecker, ytSubtitlesTool, dialogDomainService, starPaymentDomainService,
        typingIndicator);

    @Nested
    class HandleRetelling {

        @Test
        void when_handleRetelling_withYoutubeUrlAndFreeAccess_then_opensSessionAndSendsRetelling() {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            String videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
            String subtitles = "Subtitles text";

            when(accessChecker.checkAccess(client)).thenReturn(AccessChecker.AccessResult.ALLOWED_FREE);
            when(dialogDomainService.openSession(client, videoUrl)).thenReturn(sessionId);
            when(ytSubtitlesTool.loadSubtitles(videoUrl)).thenReturn(subtitles);
            when(aiClient.startRetelling(sessionId.toString(), videoUrl, subtitles, null)).thenReturn("Retelling text");

            handler.handleRetelling(chatId, client, videoUrl);

            verify(tgSender).send(chatId, "Извлекаю содержание...");
            verify(tgSender).send(chatId, "Retelling text");
            verify(tgSender).send(chatId, "Можете задавать вопросы по содержанию видео");
            verify(accessChecker).incrementDailyUsage(client);
        }

        @Test
        void when_handleRetelling_withYoutubeUrlAndAdminAccess_then_opensSessionWithoutIncrement() {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            String videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
            String subtitles = "Subtitles text";

            when(accessChecker.checkAccess(client)).thenReturn(AccessChecker.AccessResult.ALLOWED_ADMIN);
            when(dialogDomainService.openSession(client, videoUrl)).thenReturn(sessionId);
            when(ytSubtitlesTool.loadSubtitles(videoUrl)).thenReturn(subtitles);
            when(aiClient.startRetelling(sessionId.toString(), videoUrl, subtitles, null)).thenReturn("Retelling text");

            handler.handleRetelling(chatId, client, videoUrl);

            verify(tgSender).send(chatId, "Retelling text");
            verify(accessChecker, never()).incrementDailyUsage(any());
        }

        @Test
        void when_handleRetelling_withYoutubeUrlAndRequiresPayment_then_sendsInvoice() {
            Long chatId = 100L;
            Client client = Client.builder().id(UUID.randomUUID()).build();
            String videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";

            when(accessChecker.checkAccess(client)).thenReturn(AccessChecker.AccessResult.REQUIRES_PAYMENT);

            handler.handleRetelling(chatId, client, videoUrl);

            verify(tgSender).send(chatId, BotRetellingHandler.MSG_REQUIRES_PAYMENT);
            verify(starPaymentDomainService).sendInvoice(chatId, videoUrl);
            verifyNoInteractions(aiClient);
            verifyNoInteractions(dialogDomainService);
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
        void when_handleRetelling_withSubtitlesLoadingFailure_then_closesSessionAndRethrows() {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            String videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";

            when(accessChecker.checkAccess(client)).thenReturn(AccessChecker.AccessResult.ALLOWED_FREE);
            when(dialogDomainService.openSession(client, videoUrl)).thenReturn(sessionId);
            when(ytSubtitlesTool.loadSubtitles(videoUrl)).thenThrow(new RuntimeException("Subtitles loading failed"));

            org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> handler.handleRetelling(chatId, client, videoUrl)
            );

            verify(dialogDomainService).closeSession(sessionId);
            verifyNoInteractions(aiClient);
        }

        @Test
        void when_handleRetelling_withMultipleLinks_then_warningMessageSent() {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            String firstUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
            String secondUrl = "https://www.youtube.com/watch?v=Qabcdefg123";
            String inputMessage = firstUrl + " " + secondUrl;

            when(accessChecker.checkAccess(client)).thenReturn(AccessChecker.AccessResult.ALLOWED_FREE);
            when(dialogDomainService.openSession(client, firstUrl)).thenReturn(sessionId);
            when(ytSubtitlesTool.loadSubtitles(firstUrl)).thenReturn("Subtitles");
            when(aiClient.startRetelling(eq(sessionId.toString()), eq(firstUrl), anyString(), any()))
                .thenReturn("Retelling");

            handler.handleRetelling(chatId, client, inputMessage);

            verify(tgSender).send(chatId, Constants.MULTIPLE_LINKS_WARNING_MESSAGE);
        }

        @Test
        void when_handleRetelling_withMultipleLinks_then_onlyFirstProcessed() {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            String firstUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
            String secondUrl = "https://www.youtube.com/watch?v=Qabcdefg123";
            String inputMessage = firstUrl + " " + secondUrl;

            when(accessChecker.checkAccess(client)).thenReturn(AccessChecker.AccessResult.ALLOWED_FREE);
            when(dialogDomainService.openSession(client, firstUrl)).thenReturn(sessionId);
            when(ytSubtitlesTool.loadSubtitles(firstUrl)).thenReturn("Subtitles");
            when(aiClient.startRetelling(eq(sessionId.toString()), eq(firstUrl), anyString(), any()))
                .thenReturn("Retelling");

            handler.handleRetelling(chatId, client, inputMessage);

            verify(ytSubtitlesTool).loadSubtitles(firstUrl);
            verify(ytSubtitlesTool, never()).loadSubtitles(secondUrl);
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
                "Объём диалога превысил лимит токенов — разговор завершён. Пришлите новую ссылку для продолжения"
            );
        }
    }
}