package ru.panyukovnn.videoretellingbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.panyukovnn.videoretellingbot.model.content.Content;
import ru.panyukovnn.videoretellingbot.model.content.Source;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentRepository extends JpaRepository<Content, UUID> {

    Optional<Content> findTopBySourceOrderByPublicationDateDesc(Source source);

    Optional<Content> findByLink(String link);

    List<Content> findByParentBatchId(UUID childBatchId);
}
