package ru.panyukovnn.videoretellingbot.serivce.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.panyukovnn.videoretellingbot.model.Client;
import ru.panyukovnn.videoretellingbot.model.DialogSession;
import ru.panyukovnn.videoretellingbot.model.DialogSessionStatus;
import ru.panyukovnn.videoretellingbot.repository.DialogSessionRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DialogDomainService {

    private final DialogSessionRepository dialogSessionRepository;

    /**
     * Закрывает активную сессию клиента (если есть) и открывает новую.
     *
     * @return идентификатор новой сессии
     */
    @Transactional
    public UUID openSession(Client client, String videoUrl) {
        dialogSessionRepository.findByClientIdAndStatus(client.getId(), DialogSessionStatus.ACTIVE)
            .ifPresent(this::closeActiveSession);

        DialogSession newSession = DialogSession.builder()
            .clientId(client.getId())
            .videoUrl(videoUrl)
            .status(DialogSessionStatus.ACTIVE)
            .build();

        return dialogSessionRepository.save(newSession).getId();
    }

    public Optional<DialogSession> findActiveSession(UUID clientId) {
        return dialogSessionRepository.findByClientIdAndStatus(clientId, DialogSessionStatus.ACTIVE);
    }

    @Transactional
    public void closeSession(UUID sessionId) {
        dialogSessionRepository.findById(sessionId)
            .ifPresent(session -> {
                session.setStatus(DialogSessionStatus.CLOSED);
                session.setClosedAt(Instant.now());
                dialogSessionRepository.save(session);
            });
    }

    private void closeActiveSession(DialogSession session) {
        session.setStatus(DialogSessionStatus.CLOSED);
        session.setClosedAt(Instant.now());
        dialogSessionRepository.save(session);
    }
}