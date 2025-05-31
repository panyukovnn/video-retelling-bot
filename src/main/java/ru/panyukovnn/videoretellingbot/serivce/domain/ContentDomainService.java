package ru.panyukovnn.videoretellingbot.serivce.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.panyukovnn.videoretellingbot.model.content.Content;
import ru.panyukovnn.videoretellingbot.repository.ContentRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentDomainService {

    private final ContentRepository contentRepository;

    public Content save(Content content) {
        return contentRepository.save(content);
    }

    public Optional<Content> findById(UUID contentId) {
        return contentRepository.findById(contentId);
    }

    public List<Content> findByParentBatchId(UUID parentBatchId) {
        return contentRepository.findByParentBatchId(parentBatchId);
    }
}
