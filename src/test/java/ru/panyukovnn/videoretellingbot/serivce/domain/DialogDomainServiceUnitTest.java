package ru.panyukovnn.videoretellingbot.serivce.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.panyukovnn.videoretellingbot.model.Client;
import ru.panyukovnn.videoretellingbot.model.DialogSession;
import ru.panyukovnn.videoretellingbot.model.DialogSessionStatus;
import ru.panyukovnn.videoretellingbot.repository.DialogSessionRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DialogDomainServiceUnitTest {

    private final DialogSessionRepository dialogSessionRepository = mock(DialogSessionRepository.class);

    private final DialogDomainService dialogDomainService = new DialogDomainService(dialogSessionRepository);

    @Nested
    class OpenSession {

        @Test
        void when_openSession_withNoExistingSession_then_createsNewSession() {
            UUID clientId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            UUID newSessionId = UUID.randomUUID();

            when(dialogSessionRepository.findByClientIdAndStatus(clientId, DialogSessionStatus.ACTIVE))
                .thenReturn(Optional.empty());

            DialogSession savedSession = DialogSession.builder().id(newSessionId).build();
            when(dialogSessionRepository.save(any(DialogSession.class))).thenReturn(savedSession);

            UUID result = dialogDomainService.openSession(client, "https://youtube.com/watch?v=abc");

            assertEquals(newSessionId, result);

            ArgumentCaptor<DialogSession> captor = ArgumentCaptor.forClass(DialogSession.class);
            verify(dialogSessionRepository).save(captor.capture());

            DialogSession created = captor.getValue();
            assertEquals(clientId, created.getClientId());
            assertEquals("https://youtube.com/watch?v=abc", created.getVideoUrl());
            assertEquals(DialogSessionStatus.ACTIVE, created.getStatus());
        }

        @Test
        void when_openSession_withExistingActiveSession_then_closesOldAndCreatesNew() {
            UUID clientId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();

            DialogSession existingSession = DialogSession.builder()
                .id(UUID.randomUUID()).status(DialogSessionStatus.ACTIVE).build();

            when(dialogSessionRepository.findByClientIdAndStatus(clientId, DialogSessionStatus.ACTIVE))
                .thenReturn(Optional.of(existingSession));

            UUID newSessionId = UUID.randomUUID();
            when(dialogSessionRepository.save(any(DialogSession.class)))
                .thenReturn(existingSession)
                .thenReturn(DialogSession.builder().id(newSessionId).build());

            UUID result = dialogDomainService.openSession(client, "https://youtube.com/watch?v=new");

            assertEquals(newSessionId, result);
            assertEquals(DialogSessionStatus.CLOSED, existingSession.getStatus());
            assertNotNull(existingSession.getClosedAt());
            verify(dialogSessionRepository, times(2)).save(any(DialogSession.class));
        }
    }

    @Nested
    class FindActiveSession {

        @Test
        void when_findActiveSession_withExistingSession_then_returnsSession() {
            UUID clientId = UUID.randomUUID();
            DialogSession session = DialogSession.builder()
                .id(UUID.randomUUID()).status(DialogSessionStatus.ACTIVE).build();

            when(dialogSessionRepository.findByClientIdAndStatus(clientId, DialogSessionStatus.ACTIVE))
                .thenReturn(Optional.of(session));

            Optional<DialogSession> result = dialogDomainService.findActiveSession(clientId);

            assertTrue(result.isPresent());
            assertEquals(session, result.get());
        }

        @Test
        void when_findActiveSession_withNoSession_then_returnsEmpty() {
            UUID clientId = UUID.randomUUID();

            when(dialogSessionRepository.findByClientIdAndStatus(clientId, DialogSessionStatus.ACTIVE))
                .thenReturn(Optional.empty());

            Optional<DialogSession> result = dialogDomainService.findActiveSession(clientId);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class CloseSession {

        @Test
        void when_closeSession_withExistingSession_then_closesSession() {
            UUID sessionId = UUID.randomUUID();
            DialogSession session = DialogSession.builder()
                .id(sessionId).status(DialogSessionStatus.ACTIVE).build();

            when(dialogSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            dialogDomainService.closeSession(sessionId);

            assertEquals(DialogSessionStatus.CLOSED, session.getStatus());
            assertNotNull(session.getClosedAt());
            verify(dialogSessionRepository).save(session);
        }

        @Test
        void when_closeSession_withSessionNotFound_then_noUpdate() {
            UUID sessionId = UUID.randomUUID();

            when(dialogSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

            dialogDomainService.closeSession(sessionId);

            verify(dialogSessionRepository, never()).save(any());
        }
    }
}