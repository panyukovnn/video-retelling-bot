package ru.panyukovnn.videoretellingbot.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.panyukovnn.longpollingtgbotstarter.service.TgSender;

@Slf4j
@Service
@RequiredArgsConstructor
public class StartCommand {

    public static final String COMMAND = "/start";
    public static final String GREETING_MESSAGE = "Привет, я могу подготовить развернутый конспект по видео с youtube, пришли мне ссылку на видео, которое хочешь законспектировать";

    private final TgSender tgSender;

    public void execute(Long chatId) {
        tgSender.send(chatId, GREETING_MESSAGE);
    }
}