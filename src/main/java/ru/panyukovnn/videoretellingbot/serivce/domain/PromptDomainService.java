package ru.panyukovnn.videoretellingbot.serivce.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.panyukovnn.videoretellingbot.model.Prompt;
import ru.panyukovnn.videoretellingbot.repository.PromptRepository;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptDomainService {

    private final PromptRepository promptRepository;

    public Prompt save(Prompt prompt) {
        return promptRepository.save(prompt);
    }

    public Optional<Prompt> findById(UUID id) {
        return promptRepository.findById(id);
    }
}
