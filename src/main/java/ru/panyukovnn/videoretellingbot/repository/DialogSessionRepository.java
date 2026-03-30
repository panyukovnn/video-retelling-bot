package ru.panyukovnn.videoretellingbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.panyukovnn.videoretellingbot.model.DialogSession;
import ru.panyukovnn.videoretellingbot.model.DialogSessionStatus;

import java.util.Optional;
import java.util.UUID;

public interface DialogSessionRepository extends JpaRepository<DialogSession, UUID> {

    Optional<DialogSession> findByClientIdAndStatus(UUID clientId, DialogSessionStatus status);
}
