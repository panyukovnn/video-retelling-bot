package ru.panyukovnn.videoretellingbot.serivce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.panyukovnn.videoretellingbot.model.Client;
import ru.panyukovnn.videoretellingbot.property.AdminProperties;
import ru.panyukovnn.videoretellingbot.repository.ClientRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AccessChecker {

    private static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");

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

        Integer paidRemaining = client.getPaidRetellingsRemaining();

        if (paidRemaining != null && paidRemaining > 0) {
            return AccessResult.ALLOWED_PAID;
        }

        return AccessResult.REQUIRES_PAYMENT;
    }

    /**
     * Инкрементирует счётчик ежедневных пересказов после успешного бесплатного пересказа
     */
    public void incrementDailyUsage(Client client) {
        int currentUsed = client.getDailyRetellingsUsed() == null ? 0 : client.getDailyRetellingsUsed();
        client.setDailyRetellingsUsed(currentUsed + 1);
        client.setDailyRetellingsResetDate(Instant.now());
        clientRepository.save(client);
    }

    /**
     * Списывает один оплаченный пересказ после успешного выполнения
     */
    public void decrementPaidRetellings(Client client) {
        Integer remaining = client.getPaidRetellingsRemaining();

        if (remaining == null || remaining <= 0) {
            throw new IllegalStateException(
                "Невозможно списать оплаченный пересказ, остаток равен нулю, clientId: " + client.getId()
            );
        }

        client.setPaidRetellingsRemaining(remaining - 1);
        clientRepository.save(client);
    }

    private void resetDailyCounterIfNeeded(Client client) {
        LocalDate today = LocalDate.now(MOSCOW_ZONE);
        Instant resetDate = client.getDailyRetellingsResetDate();

        if (resetDate == null || !today.equals(resetDate.atZone(MOSCOW_ZONE).toLocalDate())) {
            client.setDailyRetellingsUsed(0);
            client.setDailyRetellingsResetDate(Instant.now());
            clientRepository.save(client);
        }
    }

    public enum AccessResult {
        ALLOWED_FREE,
        ALLOWED_PAID,
        ALLOWED_ADMIN,
        REQUIRES_PAYMENT
    }
}