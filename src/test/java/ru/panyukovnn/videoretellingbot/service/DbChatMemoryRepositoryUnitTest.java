package ru.panyukovnn.videoretellingbot.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import ru.panyukovnn.videoretellingbot.exception.RetellingException;
import ru.panyukovnn.videoretellingbot.model.DialogMessage;
import ru.panyukovnn.videoretellingbot.model.DialogSession;
import ru.panyukovnn.videoretellingbot.model.DialogSessionStatus;
import ru.panyukovnn.videoretellingbot.model.MessageRole;
import ru.panyukovnn.videoretellingbot.repository.DialogMessageRepository;
import ru.panyukovnn.videoretellingbot.repository.DialogSessionRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DbChatMemoryRepositoryUnitTest {

    private final DialogSessionRepository dialogSessionRepository = mock(DialogSessionRepository.class);
    private final DialogMessageRepository dialogMessageRepository = mock(DialogMessageRepository.class);

    private final DbChatMemoryRepository repository = new DbChatMemoryRepository(
        dialogSessionRepository, dialogMessageRepository
    );

    @Nested
    class FindConversationIds {

        @Test
        void when_findConversationIds_then_returnsActiveSessionIds() {
            UUID sessionId = UUID.randomUUID();
            DialogSession session = DialogSession.builder().id(sessionId).build();

            when(dialogSessionRepository.findByStatus(DialogSessionStatus.ACTIVE))
                .thenReturn(List.of(session));

            List<String> result = repository.findConversationIds();

            assertThat(result, hasSize(1));
            assertEquals(sessionId.toString(), result.get(0));
        }

        @Test
        void when_findConversationIds_withNoActiveSessions_then_returnsEmptyList() {
            when(dialogSessionRepository.findByStatus(DialogSessionStatus.ACTIVE))
                .thenReturn(List.of());

            List<String> result = repository.findConversationIds();

            assertThat(result, empty());
        }
    }

    @Nested
    class FindByConversationId {

        @Test
        void when_findByConversationId_then_returnsMappedMessages() {
            UUID sessionId = UUID.randomUUID();
            DialogSession session = DialogSession.builder().id(sessionId).build();

            DialogMessage userMsg = DialogMessage.builder()
                .session(session).role(MessageRole.USER).content("user text").build();
            DialogMessage assistantMsg = DialogMessage.builder()
                .session(session).role(MessageRole.ASSISTANT).content("assistant text").build();
            DialogMessage toolMsg = DialogMessage.builder()
                .session(session).role(MessageRole.TOOL).content("tool text").build();

            when(dialogMessageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId))
                .thenReturn(List.of(userMsg, assistantMsg, toolMsg));

            List<Message> result = repository.findByConversationId(sessionId.toString());

            assertThat(result, hasSize(3));
            assertThat(result.get(0), instanceOf(UserMessage.class));
            assertEquals("user text", result.get(0).getText());
            assertThat(result.get(1), instanceOf(AssistantMessage.class));
            assertEquals("assistant text", result.get(1).getText());
            assertThat(result.get(2), instanceOf(AssistantMessage.class));
            assertEquals("tool text", result.get(2).getText());
        }

        @Test
        void when_findByConversationId_withNoMessages_then_returnsEmptyList() {
            UUID sessionId = UUID.randomUUID();

            when(dialogMessageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId))
                .thenReturn(List.of());

            List<Message> result = repository.findByConversationId(sessionId.toString());

            assertThat(result, empty());
        }
    }

    @Nested
    class SaveAll {

        @Test
        void when_saveAll_then_deletesOldAndSavesNew() {
            UUID sessionId = UUID.randomUUID();
            DialogSession session = DialogSession.builder()
                .id(sessionId).status(DialogSessionStatus.ACTIVE).build();

            when(dialogSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            repository.saveAll(
                sessionId.toString(),
                List.of(new UserMessage("user content"), new AssistantMessage("assistant content"))
            );

            verify(dialogMessageRepository).deleteBySessionId(sessionId);

            ArgumentCaptor<List<DialogMessage>> captor = ArgumentCaptor.forClass(List.class);
            verify(dialogMessageRepository).saveAll(captor.capture());

            List<DialogMessage> saved = captor.getValue();
            assertThat(saved, hasSize(2));
            assertEquals(MessageRole.USER, saved.get(0).getRole());
            assertEquals("user content", saved.get(0).getContent());
            assertEquals(session, saved.get(0).getSession());
            assertEquals(MessageRole.ASSISTANT, saved.get(1).getRole());
            assertEquals("assistant content", saved.get(1).getContent());
        }

        @Test
        void when_saveAll_withSessionNotFound_then_throwsRetellingException() {
            UUID sessionId = UUID.randomUUID();

            when(dialogSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

            assertThrows(
                RetellingException.class,
                () -> repository.saveAll(sessionId.toString(), List.of())
            );
        }
    }

    @Nested
    class DeleteByConversationId {

        @Test
        void when_deleteByConversationId_withExistingSession_then_closesSession() {
            UUID sessionId = UUID.randomUUID();
            DialogSession session = DialogSession.builder()
                .id(sessionId).status(DialogSessionStatus.ACTIVE).build();

            when(dialogSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            repository.deleteByConversationId(sessionId.toString());

            ArgumentCaptor<DialogSession> captor = ArgumentCaptor.forClass(DialogSession.class);
            verify(dialogSessionRepository).save(captor.capture());

            DialogSession saved = captor.getValue();
            assertEquals(DialogSessionStatus.CLOSED, saved.getStatus());
            assertNotNull(saved.getClosedAt());
        }

        @Test
        void when_deleteByConversationId_withSessionNotFound_then_noUpdate() {
            UUID sessionId = UUID.randomUUID();

            when(dialogSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

            repository.deleteByConversationId(sessionId.toString());

            verify(dialogSessionRepository, never()).save(any());
        }
    }
}
