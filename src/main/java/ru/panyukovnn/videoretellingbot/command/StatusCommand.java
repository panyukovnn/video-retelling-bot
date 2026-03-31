package ru.panyukovnn.videoretellingbot.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.panyukovnn.longpollingtgbotstarter.service.TgSender;
import ru.panyukovnn.videoretellingbot.model.Client;
import ru.panyukovnn.videoretellingbot.property.AdminProperties;
import ru.panyukovnn.videoretellingbot.repository.ClientRepository;
import ru.panyukovnn.videoretellingbot.serivce.AccessChecker;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatusCommand {

    public static final String COMMAND = "/status";
    static final String MSG_ADMIN_STATUS = "Вы администратор — пересказы бесплатны без ограничений";
    static final String MSG_USER_FREE_AVAILABLE =
        "Бесплатный пересказ на сегодня: доступен. Стоимость дополнительного пересказа — 1 звезда";
    static final String MSG_USER_FREE_USED =
        "Бесплатный пересказ на сегодня: использован. Стоимость дополнительного пересказа — 1 звезда";

    private final TgSender tgSender;
    private final AdminProperties adminProperties;
    private final ClientRepository clientRepository;
    private final AccessChecker accessChecker;

    public void execute(Long chatId, Long userId) {
        String message = resolveStatusMessage(userId);

        tgSender.send(chatId, message);
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