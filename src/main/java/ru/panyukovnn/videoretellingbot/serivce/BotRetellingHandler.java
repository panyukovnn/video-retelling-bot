package ru.panyukovnn.videoretellingbot.serivce;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import ru.panyukovnn.longpollingtgbotstarter.service.StreamingMessageUpdater;
import ru.panyukovnn.longpollingtgbotstarter.service.TgSender;
import ru.panyukovnn.longpollingtgbotstarter.service.TypingIndicator;
import ru.panyukovnn.videoretellingbot.client.AiClient;
import ru.panyukovnn.videoretellingbot.exception.SubtitlesTooLongException;
import ru.panyukovnn.videoretellingbot.model.Client;
import ru.panyukovnn.videoretellingbot.model.DialogSession;
import ru.panyukovnn.videoretellingbot.serivce.domain.DialogDomainService;
import ru.panyukovnn.videoretellingbot.property.FeedbackProperties;
import ru.panyukovnn.videoretellingbot.serivce.domain.StarPaymentDomainService;
import ru.panyukovnn.videoretellingbot.tool.YtSubtitlesTool;
import ru.panyukovnn.videoretellingbot.util.Constants;
import ru.panyukovnn.videoretellingbot.util.YoutubeLinkHelper;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotRetellingHandler {

    private static final String MSG_QUESTIONS_WELCOME = "Можете задавать вопросы по содержанию видео";
    private static final String MSG_NO_SESSION = "Пришлите ссылку на YouTube-видео";
    private static final String MSG_CONTEXT_EXCEEDED =
        "Объём диалога превысил лимит токенов — разговор завершён. Пришлите новую ссылку для продолжения";
    static final String MSG_VIDEO_TOO_LONG =
        "Видео слишком длинное — объём субтитров превышает лимит модели. Попробуйте видео покороче";
    static final String MSG_REQUIRES_PAYMENT =
        "Бесплатный пересказ на сегодня уже использован. Вы можете приобрести пакет из 50 пересказов за 100 звёзд Telegram";

    private final TgSender tgSender;
    private final AiClient aiClient;
    private final AccessChecker accessChecker;
    private final YtSubtitlesTool ytSubtitlesTool;
    private final DialogDomainService dialogDomainService;
    private final StarPaymentDomainService starPaymentDomainService;
    private final TypingIndicator typingIndicator;
    private final FeedbackProperties feedbackProperties;

    public void handleRetelling(Long chatId, Client client, String inputMessage) {
        if (YoutubeLinkHelper.isValidYoutubeUrl(inputMessage)) {
            handleNewVideo(chatId, client, inputMessage, null);

            return;
        }

        Optional<String> youtubeUrl = YoutubeLinkHelper.findYoutubeUrl(inputMessage);

        if (youtubeUrl.isPresent()) {
            if (YoutubeLinkHelper.countYoutubeUrls(inputMessage) > 1) {
                tgSender.send(chatId, Constants.MULTIPLE_LINKS_WARNING_MESSAGE);
            }

            String userInstruction = YoutubeLinkHelper.extractUserInstruction(inputMessage, youtubeUrl.get())
                .orElse(null);

            handleNewVideo(chatId, client, youtubeUrl.get(), userInstruction);

            return;
        }

        handleUserQuestion(chatId, client, inputMessage);
    }

    private void handleNewVideo(Long chatId, Client client, String videoUrl, @Nullable String userInstruction) {
        AccessChecker.AccessResult accessResult = accessChecker.checkAccess(client);

        if (AccessChecker.AccessResult.REQUIRES_PAYMENT == accessResult) {
            tgSender.send(chatId, MSG_REQUIRES_PAYMENT);
            starPaymentDomainService.sendInvoice(chatId, videoUrl);

            return;
        }

        UUID sessionId = dialogDomainService.openSession(client, videoUrl);
        tgSender.send(chatId, Constants.PROCESSING_MESSAGE);

        try {
            String subtitles = loadSubtitlesWithTyping(chatId, videoUrl);
            streamRetellingToChat(chatId, sessionId.toString(), videoUrl, subtitles, userInstruction);
            tgSender.send(chatId, MSG_QUESTIONS_WELCOME);

            if (AccessChecker.AccessResult.ALLOWED_FREE == accessResult) {
                accessChecker.incrementDailyUsage(client);
            } else if (AccessChecker.AccessResult.ALLOWED_PAID == accessResult) {
                accessChecker.decrementPaidRetellings(client);
            }

            sendFeedbackMessageIfNeeded(chatId, client);
        } catch (SubtitlesTooLongException e) {
            log.warn("Субтитры видео превышают бюджет токенов. chatId: {}, videoUrl: {}", chatId, videoUrl, e);

            dialogDomainService.closeSession(sessionId);
            tgSender.send(chatId, MSG_VIDEO_TOO_LONG);
        } catch (WebClientResponseException.BadRequest e) {
            log.warn("DeepSeek отклонил запрос как слишком длинный. chatId: {}, videoUrl: {}", chatId, videoUrl, e);

            dialogDomainService.closeSession(sessionId);
            tgSender.send(chatId, MSG_VIDEO_TOO_LONG);
        } catch (Exception e) {
            dialogDomainService.closeSession(sessionId);

            throw e;
        }
    }

    private void sendFeedbackMessageIfNeeded(Long chatId, Client client) {
        int showEveryN = feedbackProperties.getShowEveryNRetellings();
        String formUrl = feedbackProperties.getFormUrl();

        if (showEveryN <= 0 || formUrl == null || formUrl.isBlank()) {
            return;
        }

        if (client.getRetellingsCount() % showEveryN == 0) {
            String feedbackMessage = String.format(Constants.FEEDBACK_MESSAGE_TEMPLATE, formUrl);
            tgSender.send(chatId, feedbackMessage);
        }
    }

    private String loadSubtitlesWithTyping(Long chatId, String videoUrl) {
        ScheduledFuture<?> typingTask = typingIndicator.start(chatId);

        try {
            return ytSubtitlesTool.loadSubtitles(videoUrl);
        } finally {
            typingIndicator.stop(typingTask);
        }
    }

    private void streamRetellingToChat(
            Long chatId,
            String sessionId,
            String videoUrl,
            String subtitles,
            @Nullable String userInstruction) {
        StreamingMessageUpdater updater = startStreamingMessage(chatId);

        try {
            aiClient.startRetellingStream(sessionId, videoUrl, subtitles, userInstruction)
                .doOnNext(updater::appendToken)
                .doOnTerminate(updater::complete)
                .blockLast();
        } finally {
            // Гарантированно останавливаем индикатор Typing, если AiClient кинул синхронное исключение до построения Flux
            updater.complete();
        }
    }

    private StreamingMessageUpdater startStreamingMessage(Long chatId) {
        try {
            return tgSender.sendStreaming(chatId);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось начать стриминг сообщения. chatId: " + chatId, e);
        }
    }

    private void handleUserQuestion(Long chatId, Client client, String userMessage) {
        Optional<DialogSession> activeSession = dialogDomainService.findActiveSession(client.getId());

        if (activeSession.isEmpty()) {
            tgSender.send(chatId, MSG_NO_SESSION);

            return;
        }

        UUID sessionId = activeSession.get().getId();

        try {
            streamDialogAnswer(chatId, sessionId.toString(), userMessage);
        } catch (SubtitlesTooLongException | WebClientResponseException.BadRequest tooLongException) {
            log.warn("Сообщение пользователя превышает бюджет токенов. sessionId: {}", sessionId, tooLongException);

            dialogDomainService.closeSession(sessionId);
            tgSender.send(chatId, MSG_VIDEO_TOO_LONG);
        } catch (Exception contextException) {
            log.warn("Превышен лимит контекста диалога. sessionId: {}", sessionId, contextException);

            dialogDomainService.closeSession(sessionId);
            tgSender.send(chatId, MSG_CONTEXT_EXCEEDED);
        }
    }

    private void streamDialogAnswer(Long chatId, String sessionId, String userMessage) {
        StreamingMessageUpdater updater = startStreamingMessage(chatId);

        try {
            aiClient.continueDialogStream(sessionId, userMessage)
                .doOnNext(updater::appendToken)
                .doOnTerminate(updater::complete)
                .blockLast();
        } finally {
            // Гарантированно останавливаем индикатор Typing, если AiClient кинул синхронное исключение до построения Flux
            updater.complete();
        }
    }
}