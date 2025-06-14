package ru.panyukovnn.videoretellingbot.serivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.panyukovnn.videoretellingbot.config.TgBotApi;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TgSender {

    private final TgBotApi botApi;
    private final TgMessagePreparer tgMessagePreparer;

    public void sendMessage(Long chatId, String message) {
        sendMessage(chatId, null, message);
    }

    public void sendMessage(Long chatId, Long messageThreadId, String message) {
        List<String> splitLongMessages = tgMessagePreparer.prepareTgMessage(message);

        splitLongMessages.forEach(splitMessage -> executeSendMessage(chatId, messageThreadId, splitMessage));
    }

    private void executeSendMessage(Long chatId, Long messageThreadId, String message) {
        SendMessage sendMessage = SendMessage.builder()
            .chatId(chatId)
            .messageThreadId(messageThreadId != null ? messageThreadId.intValue() : null)
            .disableWebPagePreview(true)
            .parseMode(ParseMode.MARKDOWN)
            .text(message)
            .build();

        try {
            botApi.execute(sendMessage);
        } catch (TelegramApiException e) {
            if (e.getMessage() != null && e.getMessage().contains("[400] Bad Request: can't parse entities")) {
                log.error("При отправке сообщения в телеграм возникла ошибка парсинга Markdown: {}", e.getMessage(), e);

                try {
                    sendMessage.setParseMode(ParseMode.HTML);
                    botApi.execute(sendMessage);
                } catch (TelegramApiException ex) {
                    log.error("Ошибка отправки сообщения в телеграм: {}", e.getMessage(), e);
                }
            } else {
                log.error("Ошибка отправки сообщения в телеграм: {}", e.getMessage(), e);
            }
        }
    }
}
