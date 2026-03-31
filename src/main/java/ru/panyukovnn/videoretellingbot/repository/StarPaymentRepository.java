package ru.panyukovnn.videoretellingbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.panyukovnn.videoretellingbot.model.StarPayment;

import java.util.UUID;

public interface StarPaymentRepository extends JpaRepository<StarPayment, UUID> {

    boolean existsByTelegramChargeId(String telegramChargeId);
}
