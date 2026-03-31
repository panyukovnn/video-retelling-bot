package ru.panyukovnn.videoretellingbot.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.panyukovnn.videoretellingbot.model.Client;
import ru.panyukovnn.videoretellingbot.property.AdminProperties;
import ru.panyukovnn.videoretellingbot.repository.ClientRepository;
import ru.panyukovnn.videoretellingbot.serivce.AccessChecker;

import java.util.Optional;

@Slf4j
@Service
public class StatusCommand extends BotCommand {

    static final String MSG_ADMIN_STATUS = "Вы администратор — пересказы бесплатны без ограничений";
    static final String MSG_USER_FREE_AVAILABLE =
        "Бесплатный пересказ на сегодня: доступен. Стоимость дополнительного пересказа — 1 звезда";
    static final String MSG_USER_FREE_USED =
        "Бесплатный пересказ на сегодня: использован. Стоимость дополнительного пересказа — 1 звезда";

    private final AdminProperties adminProperties;
    private final ClientRepository clientRepository;
    private final AccessChecker accessChecker;

    public StatusCommand(AdminProperties adminProperties,
                         ClientRepository clientRepository,
                         AccessChecker accessChecker) {
        super("status", "Проверить статус пересказов");
        this.adminProperties = adminProperties;
        this.clientRepository = clientRepository;
        this.accessChecker = accessChecker;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        String message = resolveStatusMessage(user.getId());

        try {
            absSender.execute(SendMessage.builder()
                .chatId(chat.getId())
                .text(message)
                .build());
        } catch (Exception e) {
            log.error("Ошибка при выполнении команды /status, chatId: {}", chat.getId(), e);
        }
    }

    private String resolveStatusMessage(Long userId) {
        if (adminProperties.getUserIds().contains(userId)) {
            return MSG_ADMIN_STATUS;
        }

        Optional<Client> clientOpt = clientRepository.findByTgUserId(userId);

        if (clientOpt.isEmpty()) {
            return MSG_USER_FREE_AVAILABLE;
        }

        AccessChecker.AccessResult accessResult = accessChecker.checkAccess(clientOpt.get());

        if (AccessChecker.AccessResult.REQUIRES_PAYMENT == accessResult) {
            return MSG_USER_FREE_USED;
        }

        return MSG_USER_FREE_AVAILABLE;
    }
}
