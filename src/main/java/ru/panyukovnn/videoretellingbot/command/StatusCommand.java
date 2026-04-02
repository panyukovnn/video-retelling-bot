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
        "Бесплатный пересказ на сегодня: доступен";
    static final String MSG_USER_FREE_USED =
        "Бесплатный пересказ на сегодня: использован";
    static final String MSG_PAID_RETELLINGS_REMAINING = "\nОплаченных пересказов осталось: %d";
    static final String MSG_PURCHASE_HINT = "\nВы можете приобрести пакет из 50 пересказов за 100 звёзд Telegram";

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
            return MSG_USER_FREE_AVAILABLE + MSG_PURCHASE_HINT;
        }

        Client client = clientOpt.get();
        AccessChecker.AccessResult accessResult = accessChecker.checkAccess(client);
        StringBuilder message = new StringBuilder();

        if (AccessChecker.AccessResult.REQUIRES_PAYMENT == accessResult
                || AccessChecker.AccessResult.ALLOWED_PAID == accessResult) {
            message.append(MSG_USER_FREE_USED);
        } else {
            message.append(MSG_USER_FREE_AVAILABLE);
        }

        int paidRemaining = client.getPaidRetellingsRemaining() == null
            ? 0 : client.getPaidRetellingsRemaining();

        if (paidRemaining > 0) {
            message.append(String.format(MSG_PAID_RETELLINGS_REMAINING, paidRemaining));
        } else {
            message.append(MSG_PURCHASE_HINT);
        }

        return message.toString();
    }
}