package ru.panyukovnn.videoretellingbot.serivce.eventprocessor.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.panyukovnn.videoretellingbot.client.OpenAiClient;
import ru.panyukovnn.videoretellingbot.exception.InvalidProcessingEventException;
import ru.panyukovnn.videoretellingbot.model.ConveyorTag;
import ru.panyukovnn.videoretellingbot.model.content.Content;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEvent;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEventType;
import ru.panyukovnn.videoretellingbot.property.HardcodedPromptProperties;
import ru.panyukovnn.videoretellingbot.serivce.domain.ContentDomainService;
import ru.panyukovnn.videoretellingbot.serivce.domain.ProcessingEventDomainService;
import ru.panyukovnn.videoretellingbot.serivce.eventprocessor.EventProcessor;
import ru.panyukovnn.videoretellingbot.util.JsonUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetellingEventProcessorImpl implements EventProcessor {

    private final JsonUtil jsonUtil;
    private final OpenAiClient openAiClient;
    private final ContentDomainService contentDomainService;
    private final HardcodedPromptProperties hardcodedPromptProperties;
    private final ProcessingEventDomainService processingEventDomainService;

    @Override
    public void process(ProcessingEvent processingEvent) {
        Content content = findContent(processingEvent);

        ConveyorTag tag = processingEvent.getConveyorTag();

        if (tag == null) {
            throw new InvalidProcessingEventException("8f0a", "Невозможно выполнить пересказ материала, поскольку у него отсутствует conveyorTag и невозможно определить prompt: " + jsonUtil.toJson(processingEvent));
        }

        String prompt = switch (tag) {
            case JAVA_HABR -> hardcodedPromptProperties.getJavaHabrRetelling();
            case TG_MESSAGE_BATCH -> hardcodedPromptProperties.getTgMessageBatchRetelling();
        };

        log.info("Успешно определен prompt по тегу: {}. Для материала: {}", tag, content.getTitle());

        String retellingResponse = openAiClient.promptingCall(processingEvent.getType().name(), prompt, content.getContent());

        Content retelledContent = Content.builder()
            .link(content.getLink())
            .type(content.getType())
            .source(content.getSource())
            .title(content.getTitle())
            .meta(null)
            .publicationDate(content.getPublicationDate())
            .content(retellingResponse)
            .parentBatchId(content.getChildBatchId())
            .childBatchId(null)
            .build();
        contentDomainService.save(retelledContent);

        processingEvent.setType(ProcessingEventType.PUBLISHING);
        processingEvent.setContentId(retelledContent.getId());
        processingEventDomainService.save(processingEvent);

        log.info("Успешно выполнен пересказ материала по тегу: {}. Название материала: {}", processingEvent.getType().name(), content.getTitle());
    }

    private Content findContent(ProcessingEvent processingEvent) {
        Content content = contentDomainService.findById(processingEvent.getContentId())
            .orElse(null);

        if (content == null) {
            processingEventDomainService.delete(processingEvent);

            throw new EntityNotFoundException("Не удалось выполнить пересказ материала, поскольку не найден контент, событие будет удалено");
        }

        return content;
    }

    @Override
    public ProcessingEventType getProcessingEventType() {
        return ProcessingEventType.RETELLING;
    }

}
