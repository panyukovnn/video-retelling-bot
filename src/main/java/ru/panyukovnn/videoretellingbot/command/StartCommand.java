package ru.panyukovnn.videoretellingbot.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.panyukovnn.longpollingtgbotstarter.service.TgSender;
import ru.panyukovnn.videoretellingbot.property.FeedbackProperties;
import ru.panyukovnn.videoretellingbot.util.Constants;

@Slf4j
@Service
@RequiredArgsConstructor
public class StartCommand {

    public static final String COMMAND = "/start";

    private final TgSender tgSender;
    private final FeedbackProperties feedbackProperties;

    public void execute(Long chatId) {
        String message = Constants.START_MESSAGE;
        String formUrl = feedbackProperties.getFormUrl();

        if (formUrl != null && !formUrl.isBlank()) {
            message += String.format(Constants.START_MESSAGE_FEEDBACK_SUFFIX, formUrl);
        }

        tgSender.send(chatId, message);
    }
}