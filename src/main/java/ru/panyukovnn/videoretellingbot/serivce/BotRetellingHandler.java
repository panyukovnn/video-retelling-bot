package ru.panyukovnn.videoretellingbot.serivce;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.panyukovnn.longpollingtgbotstarter.service.TgSender;
import ru.panyukovnn.videoretellingbot.client.AiClient;
import ru.panyukovnn.videoretellingbot.model.Client;
import ru.panyukovnn.videoretellingbot.model.DialogSession;
import ru.panyukovnn.videoretellingbot.serivce.domain.DialogDomainService;
import ru.panyukovnn.videoretellingbot.serivce.domain.StarPaymentDomainService;
import ru.panyukovnn.videoretellingbot.tool.YtSubtitlesTool;
import ru.panyukovnn.videoretellingbot.util.YoutubeLinkHelper;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotRetellingHandler {

    private static final String MSG_EXTRACTING = "Извлекаю содержание...";
    private static final String MSG_QUESTIONS_WELCOME = "Можете задавать вопросы по содержанию видео";
    private static final String MSG_NO_SESSION = "Пришлите ссылку на YouTube-видео";
    private static final String MSG_CONTEXT_EXCEEDED =
        "Объём диалога превысил лимит токенов — разговор завершён. Пришлите новую ссылку для продолжения";
    static final String MSG_REQUIRES_PAYMENT =
        "Бесплатный пересказ на сегодня уже использован. Стоимость одного дополнительного пересказа — 1 звезда Telegram";

    private final TgSender tgSender;
    private final AiClient aiClient;
    private final AccessChecker accessChecker;
    private final YtSubtitlesTool ytSubtitlesTool;
    private final DialogDomainService dialogDomainService;
    private final StarPaymentDomainService starPaymentDomainService;

    public void handleRetelling(Long chatId, Client client, String inputMessage) {
        if (YoutubeLinkHelper.isValidYoutubeUrl(inputMessage)) {
            handleNewVideo(chatId, client, inputMessage, null);

            return;
        }

        Optional<String> youtubeUrl = YoutubeLinkHelper.findYoutubeUrl(inputMessage);

        if (youtubeUrl.isPresent()) {
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

        tgSender.send(chatId, MSG_EXTRACTING);

        try {
            String subtitles = ytSubtitlesTool.loadSubtitles(videoUrl);
            String retelling = aiClient.startRetelling(sessionId.toString(), videoUrl, subtitles, userInstruction);

            tgSender.send(chatId, retelling);
            tgSender.send(chatId, MSG_QUESTIONS_WELCOME);

            if (AccessChecker.AccessResult.ALLOWED_FREE == accessResult) {
                accessChecker.incrementDailyUsage(client);
            }
        } catch (Exception e) {
            dialogDomainService.closeSession(sessionId);

            throw e;
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
            String answer = aiClient.continueDialog(sessionId.toString(), userMessage);

            tgSender.send(chatId, answer);
        } catch (Exception contextException) {
            log.warn("Превышен лимит контекста диалога. sessionId: {}", sessionId, contextException);

            dialogDomainService.closeSession(sessionId);
            tgSender.send(chatId, MSG_CONTEXT_EXCEEDED);
        }
    }
}
