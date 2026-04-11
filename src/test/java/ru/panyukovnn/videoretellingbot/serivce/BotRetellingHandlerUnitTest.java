package ru.panyukovnn.videoretellingbot.serivce;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import ru.panyukovnn.longpollingtgbotstarter.service.StreamingMessageUpdater;
import ru.panyukovnn.longpollingtgbotstarter.service.TgSender;
import ru.panyukovnn.longpollingtgbotstarter.service.TypingIndicator;
import ru.panyukovnn.videoretellingbot.client.AiClient;
import ru.panyukovnn.videoretellingbot.exception.SubtitlesTooLongException;
import ru.panyukovnn.videoretellingbot.model.Client;
import ru.panyukovnn.videoretellingbot.model.DialogSession;
import ru.panyukovnn.videoretellingbot.serivce.domain.DialogDomainService;
import ru.panyukovnn.videoretellingbot.serivce.domain.StarPaymentDomainService;
import ru.panyukovnn.videoretellingbot.property.FeedbackProperties;
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
    private final FeedbackProperties feedbackProperties = mock(FeedbackProperties.class);

    private final BotRetellingHandler handler = new BotRetellingHandler(
        tgSender, aiClient, accessChecker, ytSubtitlesTool, dialogDomainService, starPaymentDomainService,
        typingIndicator, feedbackProperties);

    @Nested
    class HandleRetelling {

        @Test
        void when_handleNewVideo_then_processingMessageSent() throws Exception {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            String videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";

            when(accessChecker.checkAccess(client)).thenReturn(AccessChecker.AccessResult.ALLOWED_FREE);
            when(dialogDomainService.openSession(client, videoUrl)).thenReturn(sessionId);
            when(ytSubtitlesTool.loadSubtitles(videoUrl)).thenReturn("Subtitles text");
            when(tgSender.sendStreaming(chatId)).thenReturn(mock(StreamingMessageUpdater.class));
            when(aiClient.startRetellingStream(sessionId.toString(), videoUrl, "Subtitles text", null))
                .thenReturn(Flux.just("Retelling text"));

            handler.handleRetelling(chatId, client, videoUrl);

            verify(tgSender).send(chatId, Constants.PROCESSING_MESSAGE);
        }

        @Test
        @SuppressWarnings({"unchecked", "rawtypes"})
        void when_handleNewVideo_then_typingActionSent() throws Exception {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            String videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
            ScheduledFuture typingTask = mock(ScheduledFuture.class);

            when(accessChecker.checkAccess(client)).thenReturn(AccessChecker.AccessResult.ALLOWED_FREE);
            when(dialogDomainService.openSession(client, videoUrl)).thenReturn(sessionId);
            when(ytSubtitlesTool.loadSubtitles(videoUrl)).thenReturn("Subtitles text");
            when(tgSender.sendStreaming(chatId)).thenReturn(mock(StreamingMessageUpdater.class));
            when(aiClient.startRetellingStream(sessionId.toString(), videoUrl, "Subtitles text", null))
                .thenReturn(Flux.just("Retelling text"));
            when(typingIndicator.start(chatId)).thenReturn(typingTask);

            handler.handleRetelling(chatId, client, videoUrl);

            verify(typingIndicator).start(chatId);
            verify(typingIndicator).stop(typingTask);
        }

        @Test
        void when_handleRetelling_withYoutubeUrlAndFreeAccess_then_opensSessionAndStartsStreaming() throws Exception {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            String videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
            String subtitles = "Subtitles text";

            when(accessChecker.checkAccess(client)).thenReturn(AccessChecker.AccessResult.ALLOWED_FREE);
            when(dialogDomainService.openSession(client, videoUrl)).thenReturn(sessionId);
            when(ytSubtitlesTool.loadSubtitles(videoUrl)).thenReturn(subtitles);
            when(tgSender.sendStreaming(chatId)).thenReturn(mock(StreamingMessageUpdater.class));
            when(aiClient.startRetellingStream(sessionId.toString(), videoUrl, subtitles, null))
                .thenReturn(Flux.just("Retelling text"));

            handler.handleRetelling(chatId, client, videoUrl);

            verify(tgSender).sendStreaming(chatId);
            verify(tgSender).send(chatId, Constants.PROCESSING_MESSAGE);
            verify(tgSender).send(chatId, "Можете задавать вопросы по содержанию видео");
            verify(accessChecker).incrementDailyUsage(client);
        }

        @Test
        void when_handleRetelling_withYoutubeUrlAndAdminAccess_then_opensSessionWithoutIncrement() throws Exception {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            String videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
            String subtitles = "Subtitles text";

            when(accessChecker.checkAccess(client)).thenReturn(AccessChecker.AccessResult.ALLOWED_ADMIN);
            when(dialogDomainService.openSession(client, videoUrl)).thenReturn(sessionId);
            when(ytSubtitlesTool.loadSubtitles(videoUrl)).thenReturn(subtitles);
            when(tgSender.sendStreaming(chatId)).thenReturn(mock(StreamingMessageUpdater.class));
            when(aiClient.startRetellingStream(sessionId.toString(), videoUrl, subtitles, null))
                .thenReturn(Flux.just("Retelling text"));

            handler.handleRetelling(chatId, client, videoUrl);

            verify(tgSender).sendStreaming(chatId);
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
        void when_handleRetelling_withRegularMessageAndActiveSession_then_startsStreamingDialog() throws Exception {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            DialogSession activeSession = DialogSession.builder().id(sessionId).build();

            when(dialogDomainService.findActiveSession(clientId)).thenReturn(Optional.of(activeSession));
            when(tgSender.sendStreaming(chatId)).thenReturn(mock(StreamingMessageUpdater.class));
            when(aiClient.continueDialogStream(sessionId.toString(), "What is the topic?"))
                .thenReturn(Flux.just("The topic is..."));

            handler.handleRetelling(chatId, client, "What is the topic?");

            verify(tgSender).sendStreaming(chatId);
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
        void when_handleRetelling_withMultipleLinks_then_warningMessageSent() throws Exception {
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
            when(tgSender.sendStreaming(chatId)).thenReturn(mock(StreamingMessageUpdater.class));
            when(aiClient.startRetellingStream(eq(sessionId.toString()), eq(firstUrl), anyString(), any()))
                .thenReturn(Flux.just("Retelling"));

            handler.handleRetelling(chatId, client, inputMessage);

            verify(tgSender).send(chatId, Constants.MULTIPLE_LINKS_WARNING_MESSAGE);
        }

        @Test
        void when_handleRetelling_withMultipleLinks_then_onlyFirstProcessed() throws Exception {
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
            when(tgSender.sendStreaming(chatId)).thenReturn(mock(StreamingMessageUpdater.class));
            when(aiClient.startRetellingStream(eq(sessionId.toString()), eq(firstUrl), anyString(), any()))
                .thenReturn(Flux.just("Retelling"));

            handler.handleRetelling(chatId, client, inputMessage);

            verify(ytSubtitlesTool).loadSubtitles(firstUrl);
            verify(ytSubtitlesTool, never()).loadSubtitles(secondUrl);
        }

        @Test
        void when_handleRetelling_withContextExceeded_then_closesSessionAndNotifiesUser() throws Exception {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            DialogSession activeSession = DialogSession.builder().id(sessionId).build();

            when(dialogDomainService.findActiveSession(clientId)).thenReturn(Optional.of(activeSession));
            when(tgSender.sendStreaming(chatId)).thenReturn(mock(StreamingMessageUpdater.class));
            when(aiClient.continueDialogStream(sessionId.toString(), "Next question?"))
                .thenReturn(Flux.error(new RuntimeException("Context limit exceeded")));

            handler.handleRetelling(chatId, client, "Next question?");

            verify(dialogDomainService).closeSession(sessionId);
            verify(tgSender).send(chatId,
                "Объём диалога превысил лимит токенов — разговор завершён. Пришлите новую ссылку для продолжения"
            );
        }

        @Test
        void when_handleNewVideo_withSubtitlesTooLongException_then_sendsVideoTooLongMessage() throws Exception {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            String videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";

            when(accessChecker.checkAccess(client)).thenReturn(AccessChecker.AccessResult.ALLOWED_FREE);
            when(dialogDomainService.openSession(client, videoUrl)).thenReturn(sessionId);
            when(ytSubtitlesTool.loadSubtitles(videoUrl)).thenReturn("Subtitles");
            when(tgSender.sendStreaming(chatId)).thenReturn(mock(StreamingMessageUpdater.class));
            when(aiClient.startRetellingStream(eq(sessionId.toString()), eq(videoUrl), anyString(), any()))
                .thenThrow(new SubtitlesTooLongException(100_000, 55_000));

            handler.handleRetelling(chatId, client, videoUrl);

            verify(tgSender).send(chatId, BotRetellingHandler.MSG_VIDEO_TOO_LONG);
            verify(dialogDomainService).closeSession(sessionId);
            verify(accessChecker, never()).incrementDailyUsage(any());
            verify(accessChecker, never()).decrementPaidRetellings(any());
        }

        @Test
        void when_handleNewVideo_withBadRequestFromApi_then_sendsVideoTooLongMessage() throws Exception {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            String videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
            WebClientResponseException badRequest = WebClientResponseException.create(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                HttpHeaders.EMPTY,
                new byte[0],
                null
            );

            when(accessChecker.checkAccess(client)).thenReturn(AccessChecker.AccessResult.ALLOWED_FREE);
            when(dialogDomainService.openSession(client, videoUrl)).thenReturn(sessionId);
            when(ytSubtitlesTool.loadSubtitles(videoUrl)).thenReturn("Subtitles");
            when(tgSender.sendStreaming(chatId)).thenReturn(mock(StreamingMessageUpdater.class));
            when(aiClient.startRetellingStream(eq(sessionId.toString()), eq(videoUrl), anyString(), any()))
                .thenReturn(Flux.error(badRequest));

            handler.handleRetelling(chatId, client, videoUrl);

            verify(tgSender).send(chatId, BotRetellingHandler.MSG_VIDEO_TOO_LONG);
            verify(dialogDomainService).closeSession(sessionId);
            verify(accessChecker, never()).incrementDailyUsage(any());
        }

        @Test
        void when_handleUserQuestion_withSubtitlesTooLongException_then_sendsVideoTooLongMessage() throws Exception {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            DialogSession activeSession = DialogSession.builder().id(sessionId).build();

            when(dialogDomainService.findActiveSession(clientId)).thenReturn(Optional.of(activeSession));
            when(tgSender.sendStreaming(chatId)).thenReturn(mock(StreamingMessageUpdater.class));
            when(aiClient.continueDialogStream(sessionId.toString(), "Huge question"))
                .thenThrow(new SubtitlesTooLongException(100_000, 55_000));

            handler.handleRetelling(chatId, client, "Huge question");

            verify(dialogDomainService).closeSession(sessionId);
            verify(tgSender).send(chatId, BotRetellingHandler.MSG_VIDEO_TOO_LONG);
        }

        @Test
        void when_handleNewVideo_withRetellingsCountDivisibleByN_then_feedbackMessageSent() throws Exception {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).retellingsCount(10L).build();
            String videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
            String formUrl = "https://forms.google.com/test";

            when(accessChecker.checkAccess(client)).thenReturn(AccessChecker.AccessResult.ALLOWED_FREE);
            when(dialogDomainService.openSession(client, videoUrl)).thenReturn(sessionId);
            when(ytSubtitlesTool.loadSubtitles(videoUrl)).thenReturn("Subtitles");
            when(tgSender.sendStreaming(chatId)).thenReturn(mock(StreamingMessageUpdater.class));
            when(aiClient.startRetellingStream(eq(sessionId.toString()), eq(videoUrl), anyString(), any()))
                .thenReturn(Flux.just("Retelling"));
            when(feedbackProperties.getShowEveryNRetellings()).thenReturn(5);
            when(feedbackProperties.getFormUrl()).thenReturn(formUrl);

            handler.handleRetelling(chatId, client, videoUrl);

            String expectedMessage = String.format(Constants.FEEDBACK_MESSAGE_TEMPLATE, formUrl);
            verify(tgSender).send(chatId, expectedMessage);
        }

        @Test
        void when_handleNewVideo_withRetellingsCountNotDivisible_then_feedbackMessageNotSent() throws Exception {
            Long chatId = 100L;
            UUID clientId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).retellingsCount(7L).build();
            String videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
            String formUrl = "https://forms.google.com/test";

            when(accessChecker.checkAccess(client)).thenReturn(AccessChecker.AccessResult.ALLOWED_FREE);
            when(dialogDomainService.openSession(client, videoUrl)).thenReturn(sessionId);
            when(ytSubtitlesTool.loadSubtitles(videoUrl)).thenReturn("Subtitles");
            when(tgSender.sendStreaming(chatId)).thenReturn(mock(StreamingMessageUpdater.class));
            when(aiClient.startRetellingStream(eq(sessionId.toString()), eq(videoUrl), anyString(), any()))
                .thenReturn(Flux.just("Retelling"));
            when(feedbackProperties.getShowEveryNRetellings()).thenReturn(5);
            when(feedbackProperties.getFormUrl()).thenReturn(formUrl);

            handler.handleRetelling(chatId, client, videoUrl);

            String feedbackMessage = String.format(Constants.FEEDBACK_MESSAGE_TEMPLATE, formUrl);
            verify(tgSender, never()).send(chatId, feedbackMessage);
        }
    }
}
