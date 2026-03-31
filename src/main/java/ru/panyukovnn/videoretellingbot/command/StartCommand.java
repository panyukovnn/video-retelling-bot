package ru.panyukovnn.videoretellingbot.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.panyukovnn.longpollingtgbotstarter.service.TgSender;
import ru.panyukovnn.videoretellingbot.util.Constants;

@Slf4j
@Service
@RequiredArgsConstructor
public class StartCommand {

    public static final String COMMAND = "/start";

    private final TgSender tgSender;

    public void execute(Long chatId) {
        tgSender.send(chatId, Constants.START_MESSAGE);
    }
}