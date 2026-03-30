package ru.panyukovnn.videoretellingbot.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.panyukovnn.longpollingtgbotstarter.service.TgSender;
import ru.panyukovnn.videoretellingbot.dto.UpdateParams;
import ru.panyukovnn.videoretellingbot.exception.RetellingException;
import ru.panyukovnn.videoretellingbot.model.Client;
import ru.panyukovnn.videoretellingbot.serivce.BotRetellingHandler;
import ru.panyukovnn.videoretellingbot.serivce.domain.ClientDomainService;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TgBotListener {

    private final TgSender tgSender;
    private final ClientDomainService clientDomainService;
    private final BotRetellingHandler botRetellingHandler;

    @Async("tgListenerExecutor")
    @EventListener(Update.class)
    public CompletableFuture<Void> onUpdate(Update update) {
        try {
            UpdateParams updateParams = crateUpdateParams(update).orElse(null);

            if (updateParams == null) {
                log.warn("Не удалось прочитать сообщение телеграм: {}", update);

                return CompletableFuture.completedFuture(null);
            }

            if (!Objects.equals(updateParams.getChatId(), updateParams.getUserId())) {
                log.warn("Получено сообщение не из приватного чата: {}", updateParams);

                return CompletableFuture.completedFuture(null);
            }

            MDC.clear();
            MDC.put("input", updateParams.getInput());
            MDC.put("userId", String.valueOf(updateParams.getUserId()));
            MDC.put("fullUserName", formatFullUserName(updateParams));
            MDC.put("traceId", UUID.randomUUID().toString());

            log.info(update.toString());

            try {
                if (updateParams.getInput() == null) {
                    log.warn("Получено некорректное сообщение в телеграм: {}", update);

                    return CompletableFuture.completedFuture(null);
                }

                Client client = clientDomainService.save(updateParams);

                botRetellingHandler.handleRetelling(updateParams.getChatId(), client, updateParams.getInput());
            } catch (RetellingException e) {
                log.error("Ошибка бизнес логики. id: {}. Сообщение: {}", e.getId(), e.getMessage(), e);

                tgSender.send(updateParams.getChatId(), "В процессе работы возникла ошибка: " + e.getMessage());
            } catch (Exception e) {
                log.error(e.getMessage(), e);

                tgSender.send(updateParams.getChatId(), "Непредвиденная ошибка при отправке сообщения");
            }
        } finally {
            MDC.clear();
        }

        return CompletableFuture.completedFuture(null);
    }

    protected Optional<UpdateParams> crateUpdateParams(Update update) {
        if (update.hasCallbackQuery()) {
            User user = update.getCallbackQuery().getFrom();

            return Optional.of(new UpdateParams(
                user.getId(),
                update.getCallbackQuery().getMessage().getChatId(),
                update.getCallbackQuery().getMessage().getMessageThreadId(),
                user.getUserName(),
                user.getFirstName(),
                user.getLastName(),
                null,
                Instant.ofEpochSecond(update.getCallbackQuery().getMessage().getDate()),
                update.getCallbackQuery().getData()));
        }

        if (update.getMessage() == null) {
            return Optional.empty();
        }

        User user = update.getMessage().getFrom();

        return Optional.of(new UpdateParams(
            user.getId(),
            update.getMessage().getChatId(),
            update.getMessage().getMessageThreadId(),
            user.getUserName(),
            user.getFirstName(),
            user.getLastName(),
            update.getMessage().getText(),
            Instant.ofEpochSecond(update.getMessage().getDate()),
            null));
    }

    protected String formatFullUserName(UpdateParams updateParams) {
        if (updateParams == null) {
            return "";
        }

        List<String> fullUserNameParts = new ArrayList<>();

        Optional.ofNullable(updateParams.getLastname()).filter(StringUtils::hasText).ifPresent(fullUserNameParts::add);
        Optional.ofNullable(updateParams.getFirstname()).filter(StringUtils::hasText).ifPresent(fullUserNameParts::add);
        Optional.ofNullable(updateParams.getUserName()).filter(StringUtils::hasText).ifPresent(fullUserNameParts::add);

        return String.join(" ", fullUserNameParts);
    }

}