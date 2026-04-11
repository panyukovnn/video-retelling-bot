package ru.panyukovnn.videoretellingbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.panyukovnn.videoretellingbot.model.TokenUsage;

import java.util.UUID;

public interface TokenUsageRepository extends JpaRepository<TokenUsage, UUID> {
}