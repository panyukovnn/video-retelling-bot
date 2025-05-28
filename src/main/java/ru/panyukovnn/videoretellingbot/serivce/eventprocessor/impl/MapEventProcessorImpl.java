package ru.panyukovnn.videoretellingbot.serivce.eventprocessor.impl;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.panyukovnn.videoretellingbot.client.OpenAiClient;
import ru.panyukovnn.videoretellingbot.exception.InvalidProcessingEventException;
import ru.panyukovnn.videoretellingbot.model.Prompt;
import ru.panyukovnn.videoretellingbot.model.PublishingChannel;
import ru.panyukovnn.videoretellingbot.model.content.Content;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEvent;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEventType;
import ru.panyukovnn.videoretellingbot.repository.ContentRepository;
import ru.panyukovnn.videoretellingbot.serivce.domain.ProcessingEventDomainService;
import ru.panyukovnn.videoretellingbot.serivce.domain.PromptDomainService;
import ru.panyukovnn.videoretellingbot.serivce.domain.PublishingChannelDomainService;
import ru.panyukovnn.videoretellingbot.serivce.eventprocessor.EventProcessor;
import ru.panyukovnn.videoretellingbot.serivce.telegram.TgSender;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MapEventProcessorImpl implements EventProcessor {

    private final TgSender tgSender;
    private final OpenAiClient openAiClient;
    private final ContentRepository contentRepository;
    private final PromptDomainService promptDomainService;
    private final ProcessingEventDomainService processingEventDomainService;
    private final PublishingChannelDomainService publishingChannelDomainService;

    @Override
    public void process(ProcessingEvent processingEvent) {
        Content content = contentRepository.findById(processingEvent.getContentId())
            .orElseThrow(() -> new InvalidProcessingEventException("42d6", "Не удалось найти контент"));
        PublishingChannel publishingChannel = publishingChannelDomainService.findById(processingEvent.getPublishingChannelId())
            .orElseThrow(() -> new InvalidProcessingEventException("4f66", "Не удалось найти канал публикации"));
        Prompt prompt = promptDomainService.findById(processingEvent.getPromptId())
            .orElseThrow(() -> new InvalidProcessingEventException("b475", "Не удалось найти промты"));

        String mappedResponse = openAiClient.promptingCall(
            processingEvent.getType().name(),
            prompt.getMapPrompt(),
            content.getContent());

        // TODO сохранять куда-то в бд и передавать на шаг публикации

        log.info("Успешно выполнен пересказ материала по тегу: {}. Название материала: {}", processingEvent.getType().name(), content.getTitle());

        try {
            String contentTitle = content.getTitle();
            String formattedMessage = formatMessage(content, mappedResponse);

            tgSender.sendMessage(publishingChannel.getChatId(), publishingChannel.getTopicId(), formattedMessage);

            log.info("Успешно выполнена отправка преобразованного материала. Название материала: {}. contentId: {}", contentTitle, content.getId());

            processingEvent.setType(ProcessingEventType.PUBLISHED);
        } catch (Exception e) {
            log.error("Ошибка при отправке пересказа в телеграм: {}", e.getMessage(), e);

            processingEvent.setType(ProcessingEventType.PUBLICATION_ERROR);
        } finally {
            processingEventDomainService.save(processingEvent);
        }
    }

    @Override
    public ProcessingEventType getProcessingEventType() {
        return ProcessingEventType.MAP;
    }

    private static String formatMessage(@Nullable Content content, String retelling) {
        if (content == null || content.getLink() == null) {
            return retelling;
        }

        String title = Optional.of(content)
            .map(Content::getTitle)
            .filter(StringUtils::hasText)
            .orElse("Ссылка");

        String firstLine = title + "\n" + content.getLink() + "\n\n";

        return firstLine + retelling;
    }

}
