package ru.panyukovnn.videoretellingbot.serivce;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.panyukovnn.videoretellingbot.model.Client;
import ru.panyukovnn.videoretellingbot.property.AdminProperties;
import ru.panyukovnn.videoretellingbot.repository.ClientRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccessCheckerUnitTest {

    private final AdminProperties adminProperties = new AdminProperties();
    private final ClientRepository clientRepository = mock(ClientRepository.class);

    private final AccessChecker accessChecker = new AccessChecker(adminProperties, clientRepository);

    @Nested
    class CheckAccess {

        @Test
        void when_checkAccess_withAdminUser_then_returnsAllowedAdmin() {
            Long adminUserId = 12345L;
            adminProperties.setUserIds(List.of(adminUserId));

            Client client = Client.builder()
                .id(UUID.randomUUID())
                .tgUserId(adminUserId)
                .dailyRetellingsUsed(5)
                .build();

            AccessChecker.AccessResult result = accessChecker.checkAccess(client);

            assertEquals(AccessChecker.AccessResult.ALLOWED_ADMIN, result);
        }

        @Test
        void when_checkAccess_withFirstRetellToday_then_returnsAllowedFree() {
            Client client = Client.builder()
                .id(UUID.randomUUID())
                .tgUserId(100L)
                .dailyRetellingsUsed(0)
                .dailyRetellingsResetDate(LocalDateTime.now())
                .build();

            AccessChecker.AccessResult result = accessChecker.checkAccess(client);

            assertEquals(AccessChecker.AccessResult.ALLOWED_FREE, result);
        }

        @Test
        void when_checkAccess_withSecondRetellToday_then_returnsRequiresPayment() {
            Client client = Client.builder()
                .id(UUID.randomUUID())
                .tgUserId(100L)
                .dailyRetellingsUsed(1)
                .dailyRetellingsResetDate(LocalDateTime.now())
                .build();

            AccessChecker.AccessResult result = accessChecker.checkAccess(client);

            assertEquals(AccessChecker.AccessResult.REQUIRES_PAYMENT, result);
        }

        @Test
        void when_checkAccess_afterDayChange_then_resetsCounterAndReturnsAllowedFree() {
            Client client = Client.builder()
                .id(UUID.randomUUID())
                .tgUserId(100L)
                .dailyRetellingsUsed(3)
                .dailyRetellingsResetDate(LocalDateTime.now().minusDays(1))
                .build();

            AccessChecker.AccessResult result = accessChecker.checkAccess(client);

            assertEquals(AccessChecker.AccessResult.ALLOWED_FREE, result);
            assertEquals(0, client.getDailyRetellingsUsed());
            assertEquals(LocalDate.now(), client.getDailyRetellingsResetDate().toLocalDate());
            verify(clientRepository).save(client);
        }

        @Test
        void when_checkAccess_withNullDailyUsed_then_returnsAllowedFree() {
            Client client = Client.builder()
                .id(UUID.randomUUID())
                .tgUserId(100L)
                .dailyRetellingsUsed(null)
                .dailyRetellingsResetDate(null)
                .build();

            AccessChecker.AccessResult result = accessChecker.checkAccess(client);

            assertEquals(AccessChecker.AccessResult.ALLOWED_FREE, result);
        }
    }

    @Nested
    class IncrementDailyUsage {

        @Test
        void when_incrementDailyUsage_then_incrementsCounterAndSaves() {
            Client client = Client.builder()
                .id(UUID.randomUUID())
                .tgUserId(100L)
                .dailyRetellingsUsed(0)
                .build();

            accessChecker.incrementDailyUsage(client);

            assertEquals(1, client.getDailyRetellingsUsed());
            assertNotNull(client.getDailyRetellingsResetDate());
            assertEquals(LocalDate.now(), client.getDailyRetellingsResetDate().toLocalDate());
            verify(clientRepository).save(client);
        }
    }
}