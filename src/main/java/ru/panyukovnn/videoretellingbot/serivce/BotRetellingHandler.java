package ru.panyukovnn.videoretellingbot.serivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.panyukovnn.videoretellingbot.client.OpenAiClient;
import ru.panyukovnn.videoretellingbot.dto.YoutubeSubtitles;
import ru.panyukovnn.videoretellingbot.exception.RetellingException;
import ru.panyukovnn.videoretellingbot.property.PromptProperties;
import ru.panyukovnn.videoretellingbot.util.YoutubeLinkHelper;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotRetellingHandler {

    private final TgSender tgSender;
    private final OpenAiClient openAiClient;
    private final PromptProperties promptProperties;
    private final YoutubeSubtitlesLoader youtubeSubtitlesLoader;

    public void handleRetelling(Long chatId, String inputMessage) {
        if (!YoutubeLinkHelper.isValidYoutubeUrl(inputMessage)) {
            throw new RetellingException("824c", "Невалидная ссылка youtube: " + inputMessage);
        }

        String cleanedYoutubeLink = YoutubeLinkHelper.removeRedundantQueryParamsFromYoutubeLint(inputMessage);

        tgSender.sendMessage(chatId, "Извлекаю содержание");

        YoutubeSubtitles youtubeSubtitles = youtubeSubtitlesLoader.load(cleanedYoutubeLink);

        String subtitles = youtubeSubtitles.getSubtitles();

        tgSender.sendMessage(chatId, "Формирую статью (это может занимать до 2х минут)");

        String retellingResponse = openAiClient.promptingCall("retelling_from_bot", promptProperties.getYoutubeRetelling(), subtitles);

        try {
            tgSender.sendMessage(chatId, retellingResponse);

            log.info("Пересказ успешно выполнен и доставлен");
        } catch (RetellingException e) {
            log.warn("Ошибка бизнес логики. id: {}. Сообщение: {}", e.getId(), e.getMessage(), e);

            tgSender.sendMessage(chatId, "В процессе работы возникла ошибка: " + e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            tgSender.sendMessage(chatId, "Непредвиденная ошибка при отправке сообщения");
        }
    }
}
