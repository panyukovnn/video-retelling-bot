package ru.panyukovnn.videoretellingbot.serivce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.panyukovnn.videoretellingbot.model.Client;
import ru.panyukovnn.videoretellingbot.property.AdminProperties;
import ru.panyukovnn.videoretellingbot.repository.ClientRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccessChecker {

    private final AdminProperties adminProperties;
    private final ClientRepository clientRepository;

    public AccessResult checkAccess(Client client) {
        if (adminProperties.getUserIds().contains(client.getTgUserId())) {
            return AccessResult.ALLOWED_ADMIN;
        }

        resetDailyCounterIfNeeded(client);

        if (client.getDailyRetellingsUsed() == null || client.getDailyRetellingsUsed() == 0) {
            return AccessResult.ALLOWED_FREE;
        }

        return AccessResult.REQUIRES_PAYMENT;
    }

    /**
     * Инкрементирует счётчик ежедневных пересказов после успешного бесплатного пересказа
     */
    public void incrementDailyUsage(Client client) {
        int currentUsed = client.getDailyRetellingsUsed() == null ? 0 : client.getDailyRetellingsUsed();
        client.setDailyRetellingsUsed(currentUsed + 1);
        client.setDailyRetellingsResetDate(LocalDateTime.now());
        clientRepository.save(client);
    }

    private void resetDailyCounterIfNeeded(Client client) {
        LocalDate today = LocalDate.now();
        LocalDateTime resetDate = client.getDailyRetellingsResetDate();

        if (resetDate == null || !today.equals(resetDate.toLocalDate())) {
            client.setDailyRetellingsUsed(0);
            client.setDailyRetellingsResetDate(LocalDateTime.now());
            clientRepository.save(client);
        }
    }

    public enum AccessResult {
        ALLOWED_FREE,
        ALLOWED_ADMIN,
        REQUIRES_PAYMENT
    }
}
