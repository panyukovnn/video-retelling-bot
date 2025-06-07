package ru.panyukovnn.videoretellingbot.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Slf4j
@Service
public class SourceCommand extends BotCommand {

    public SourceCommand() {
        super("source", "Source command");
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        try {
            if (strings.length < 2) {
                log.error("В команду source полступил запрос с некорректным количеством полей");
                // TODO send in reponse

                return;
            }
//            if ( ) TODO проверить ялвяется ли пользователь администратором

            // Отправить запрос на выгрузку контента
            // Выгрузить весь контент, сохранить в бд
            // Сохранить publication_channel
            // Теперь при получении команды /prompt в этом канале публикации - создается processing_event

//            absSender.execute(SendMessage.builder()
//                    .chatId(chat.getId())
//                    .text(GREETING_MESSAGE)
//                    .build());
        } catch (Exception e) {
            log.error("Exception at start command {}: {}", chat.getId(), e.getMessage(), e);
        }
    }
}
