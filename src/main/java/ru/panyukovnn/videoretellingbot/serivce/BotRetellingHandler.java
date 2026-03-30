package ru.panyukovnn.videoretellingbot.serivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.panyukovnn.longpollingtgbotstarter.service.TgSender;
import ru.panyukovnn.videoretellingbot.client.AiClient;
import ru.panyukovnn.videoretellingbot.exception.RetellingException;
import ru.panyukovnn.videoretellingbot.property.PromptProperties;
import ru.panyukovnn.videoretellingbot.util.YoutubeLinkHelper;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotRetellingHandler {

    private final TgSender tgSender;
    private final AiClient aiClient;
    private final PromptProperties promptProperties;

    public void handleRetelling(Long chatId, String inputMessage) {
        if (!YoutubeLinkHelper.isValidYoutubeUrl(inputMessage)) {
            throw new RetellingException("824c", "Невалидная ссылка youtube: " + inputMessage);
        }

        tgSender.send(chatId, "Извлекаю содержание");

        tgSender.send(chatId, "Формирую пересказ (это может занимать до 2х минут)");

        String retellingResponse = aiClient.promptingCall("retelling_from_bot", promptProperties.getYoutubeRetelling(), inputMessage);

        try {
            tgSender.send(chatId, retellingResponse);

            log.info("Пересказ успешно выполнен и доставлен");
        } catch (RetellingException e) {
            log.warn("Ошибка бизнес логики. id: {}. Сообщение: {}", e.getId(), e.getMessage(), e);

            tgSender.send(chatId, "В процессе работы возникла ошибка: " + e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            tgSender.send(chatId, "Непредвиденная ошибка при отправке сообщения");
        }
    }
}
