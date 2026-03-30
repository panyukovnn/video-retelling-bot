package ru.panyukovnn.videoretellingbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.panyukovnn.videoretellingbot.exception.RetellingException;
import ru.panyukovnn.videoretellingbot.model.DialogMessage;
import ru.panyukovnn.videoretellingbot.model.DialogSession;
import ru.panyukovnn.videoretellingbot.model.DialogSessionStatus;
import ru.panyukovnn.videoretellingbot.model.MessageRole;
import ru.panyukovnn.videoretellingbot.repository.DialogMessageRepository;
import ru.panyukovnn.videoretellingbot.repository.DialogSessionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DbChatMemoryRepository implements ChatMemoryRepository {

    private final DialogSessionRepository dialogSessionRepository;
    private final DialogMessageRepository dialogMessageRepository;

    @Override
    public List<String> findConversationIds() {
        return dialogSessionRepository.findByStatus(DialogSessionStatus.ACTIVE)
            .stream()
            .map(session -> session.getId().toString())
            .toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        UUID sessionId = UUID.fromString(conversationId);
        List<DialogMessage> messages = dialogMessageRepository
            .findBySessionIdOrderByCreateTimeAsc(sessionId);

        return messages.stream()
            .map(this::toSpringAiMessage)
            .toList();
    }

    /**
     * Заменяет все существующие сообщения сессии на переданные.
     * Поведение соответствует контракту интерфейса ChatMemoryRepository.
     */
    @Override
    @Transactional
    public void saveAll(String conversationId, List<Message> messages) {
        UUID sessionId = UUID.fromString(conversationId);
        DialogSession session = dialogSessionRepository.findById(sessionId)
            .orElseThrow(() -> new RetellingException(
                "f769", "Сессия диалога не найдена по идентификатору: " + conversationId
            ));

        dialogMessageRepository.deleteBySessionId(sessionId);

        List<DialogMessage> dialogMessages = messages.stream()
            .map(message -> toDialogMessage(message, session))
            .toList();

        dialogMessageRepository.saveAll(dialogMessages);
    }

    @Override
    @Transactional
    public void deleteByConversationId(String conversationId) {
        UUID sessionId = UUID.fromString(conversationId);

        dialogSessionRepository.findById(sessionId)
            .ifPresent(session -> {
                session.setStatus(DialogSessionStatus.CLOSED);
                session.setClosedAt(LocalDateTime.now());
                dialogSessionRepository.save(session);
            });
    }

    private DialogMessage toDialogMessage(Message message, DialogSession session) {
        return DialogMessage.builder()
            .session(session)
            .role(toMessageRole(message.getMessageType()))
            .content(message.getText())
            .build();
    }

    private MessageRole toMessageRole(MessageType messageType) {
        return switch (messageType) {
            case USER -> MessageRole.USER;
            case ASSISTANT -> MessageRole.ASSISTANT;
            case TOOL -> MessageRole.TOOL;
            default -> throw new RetellingException(
                "4f06", "Неподдерживаемый тип сообщения при сохранении в БД: " + messageType
            );
        };
    }

    private Message toSpringAiMessage(DialogMessage dialogMessage) {
        return switch (dialogMessage.getRole()) {
            case USER -> new UserMessage(dialogMessage.getContent());
            case ASSISTANT, TOOL -> new AssistantMessage(dialogMessage.getContent());
        };
    }
}
