package ru.panyukovnn.videoretellingbot.serivce.eventprocessor.impl;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.panyukovnn.videoretellingbot.exception.InvalidProcessingEventException;
import ru.panyukovnn.videoretellingbot.model.ConveyorTag;
import ru.panyukovnn.videoretellingbot.model.content.Content;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEvent;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEventType;
import ru.panyukovnn.videoretellingbot.model.retelling.Retelling;
import ru.panyukovnn.videoretellingbot.property.ConveyorTagProperties;
import ru.panyukovnn.videoretellingbot.property.PublishingProperties;
import ru.panyukovnn.videoretellingbot.repository.ContentRepository;
import ru.panyukovnn.videoretellingbot.repository.RetellingRepository;
import ru.panyukovnn.videoretellingbot.serivce.domain.ProcessingEventDomainService;
import ru.panyukovnn.videoretellingbot.serivce.eventprocessor.EventProcessor;
import ru.panyukovnn.videoretellingbot.serivce.telegram.TgSender;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishRetellingEventProcessorImpl implements EventProcessor {

    private final TgSender tgSender;
    private final ContentRepository contentRepository;
    private final RetellingRepository retellingRepository;
    private final PublishingProperties publishingProperties;
    private final ConveyorTagProperties conveyorTagProperties;
    private final ProcessingEventDomainService processingEventDomainService;

    @Override
    public void process(ProcessingEvent processingEvent) {
        Retelling retelling = retellingRepository.findById(processingEvent.getRetellingId())
            .orElseThrow(() -> new InvalidProcessingEventException("9f9f", "Не удалось выполнить публикацию пересказа, поскольку не найден пересказ, событие будет удалено"));

        try {
            Content content = contentRepository.findById(retelling.getContentId())
                .orElse(null);
            String contentTitle = content != null ? content.getTitle() : "undefined";

            String formattedMessage = formatMessage(content, retelling.getRetelling());

            ConveyorTag tag = processingEvent.getConveyorTag();
            ConveyorTagProperties.ConveyorTagConfig conveyorTagConfig = conveyorTagProperties.getWithGuarantee(tag);

            Long messageThreadId = conveyorTagConfig.getPublishingTopicId();
            if (messageThreadId == null) {
                log.error("Не удалось определить messageThreadId по тегу: {}. Для материала: {}", tag, contentTitle);

                return;
            }

            tgSender.sendMessage(publishingProperties.getChatId(), messageThreadId, formattedMessage);

            log.info("Успешно выполнена отправка пересказа материала. Конвейер: {}. Тег: {}. Название материала: {}",
                processingEvent.getConveyorType(),
                processingEvent.getConveyorTag(), contentTitle);

            processingEvent.setType(ProcessingEventType.PUBLISHED);
        } catch (Exception e) {
            log.error("Ошибка при отправке пересказа в телеграм: {}", e.getMessage(), e);

            processingEvent.setType(ProcessingEventType.PUBLICATION_ERROR);
        }

        processingEventDomainService.save(processingEvent);
    }

    @Override
    public ProcessingEventType getProcessingEventType() {
        return ProcessingEventType.PUBLISH_RETELLING;
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
