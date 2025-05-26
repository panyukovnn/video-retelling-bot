package ru.panyukovnn.videoretellingbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.panyukovnn.videoretellingbot.model.Prompt;

import java.util.UUID;

public interface PromptRepository extends JpaRepository<Prompt, UUID> {

}
