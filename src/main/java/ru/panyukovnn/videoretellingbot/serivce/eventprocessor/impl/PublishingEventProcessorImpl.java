package ru.panyukovnn.videoretellingbot.serivce.eventprocessor.impl;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.panyukovnn.videoretellingbot.exception.InvalidProcessingEventException;
import ru.panyukovnn.videoretellingbot.model.PublishingChannel;
import ru.panyukovnn.videoretellingbot.model.content.Content;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEvent;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEventType;
import ru.panyukovnn.videoretellingbot.serivce.domain.ContentDomainService;
import ru.panyukovnn.videoretellingbot.serivce.domain.ProcessingEventDomainService;
import ru.panyukovnn.videoretellingbot.serivce.domain.PublishingChannelDomainService;
import ru.panyukovnn.videoretellingbot.serivce.eventprocessor.EventProcessor;
import ru.panyukovnn.videoretellingbot.serivce.telegram.TgSender;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishingEventProcessorImpl implements EventProcessor {

    private final TgSender tgSender;
    private final ContentDomainService contentDomainService;
    private final ProcessingEventDomainService processingEventDomainService;
    private final PublishingChannelDomainService publishingChannelDomainService;

    @Override
    public void process(ProcessingEvent processingEvent) {
        PublishingChannel publishingChannel = publishingChannelDomainService.findById(processingEvent.getPublishingChannelId())
            .orElseThrow(() -> new InvalidProcessingEventException("4df0", "Не удалось найти данные о канале публикации"));
        Content content = contentDomainService.findById(processingEvent.getContentId())
            .orElseThrow(() -> new InvalidProcessingEventException("42d6", "Не удалось найти контент"));

        try {
            String contentTitle = content.getTitle();
            String formattedMessage = formatMessage(content, content.getContent());

            tgSender.sendMessage(publishingChannel.getChatId(), publishingChannel.getTopicId(), formattedMessage);

            log.info("Успешно выполнена отправка reduced материала. Название материала: {}. contentId: {}", contentTitle, content.getId());

            processingEvent.setType(ProcessingEventType.PUBLISHED);
        } catch (Exception e) {
            log.error("Ошибка при отправке reduced материала в телеграм: {}", e.getMessage(), e);

            processingEvent.setType(ProcessingEventType.PUBLICATION_ERROR);
        } finally {
            processingEventDomainService.save(processingEvent);
        }
    }

    @Override
    public ProcessingEventType getProcessingEventType() {
        return ProcessingEventType.PUBLISHING;
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
