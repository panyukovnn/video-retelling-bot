package ru.panyukovnn.videoretellingbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.panyukovnn.videoretellingbot.model.PublishingChannel;

import java.util.Optional;
import java.util.UUID;

public interface PublishingChannelRepository extends JpaRepository<PublishingChannel, UUID> {

    Optional<PublishingChannel> findByExternalId(String externalId);
}
