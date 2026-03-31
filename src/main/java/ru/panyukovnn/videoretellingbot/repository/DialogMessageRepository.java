package ru.panyukovnn.videoretellingbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.panyukovnn.videoretellingbot.model.DialogMessage;

import java.util.List;
import java.util.UUID;

public interface DialogMessageRepository extends JpaRepository<DialogMessage, UUID> {

    List<DialogMessage> findBySessionIdOrderByCreateTimeAsc(UUID sessionId);

    void deleteBySessionId(UUID sessionId);
}
