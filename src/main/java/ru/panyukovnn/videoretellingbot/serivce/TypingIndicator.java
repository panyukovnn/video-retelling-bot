package ru.panyukovnn.videoretellingbot.serivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TypingIndicator {

    private static final int TYPING_REFRESH_INTERVAL_SECONDS = 4;

    private final TelegramClient telegramClient;
    private final ScheduledExecutorService typingSchedulerExecutor;

    /**
     * Отправляет действие typing немедленно и запускает периодическую отправку каждые 4 секунды.
     * Возвращает задачу для последующей отмены через {@link #stop(ScheduledFuture)}.
     */
    public ScheduledFuture<?> start(Long chatId) {
        sendTypingAction(chatId);

        return typingSchedulerExecutor.scheduleAtFixedRate(
            () -> sendTypingAction(chatId),
            TYPING_REFRESH_INTERVAL_SECONDS,
            TYPING_REFRESH_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
    }

    /**
     * Останавливает периодическую отправку статуса typing.
     */
    public void stop(ScheduledFuture<?> typingTask) {
        if (typingTask != null) {
            typingTask.cancel(false);
        }
    }

    private void sendTypingAction(Long chatId) {
        try {
            telegramClient.execute(SendChatAction.builder()
                .chatId(String.valueOf(chatId))
                .action("typing")
                .build());
        } catch (Exception e) {
            log.warn("Не удалось отправить статус typing для чата {}", chatId, e);
        }
    }
}