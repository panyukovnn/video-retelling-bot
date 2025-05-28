package ru.panyukovnn.videoretellingbot.serivce.eventprocessor.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.panyukovnn.videoretellingbot.client.OpenAiClient;
import ru.panyukovnn.videoretellingbot.model.ConveyorTag;
import ru.panyukovnn.videoretellingbot.model.content.Content;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEvent;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEventType;
import ru.panyukovnn.videoretellingbot.model.retelling.Retelling;
import ru.panyukovnn.videoretellingbot.property.ConveyorTagProperties;
import ru.panyukovnn.videoretellingbot.repository.ContentRepository;
import ru.panyukovnn.videoretellingbot.repository.RetellingRepository;
import ru.panyukovnn.videoretellingbot.serivce.domain.ProcessingEventDomainService;
import ru.panyukovnn.videoretellingbot.serivce.eventprocessor.EventProcessor;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetellingEventProcessorImpl implements EventProcessor {

    private final OpenAiClient openAiClient;
    private final ContentRepository contentRepository;
    private final RetellingRepository retellingRepository;
    private final ConveyorTagProperties conveyorTagProperties;
    private final ProcessingEventDomainService processingEventDomainService;

    @Override
    public void process(ProcessingEvent processingEvent) {
        Content content = findContent(processingEvent);

        ConveyorTag tag = processingEvent.getConveyorTag();
        ConveyorTagProperties.ConveyorTagConfig conveyorTagConfig = conveyorTagProperties.getWithGuarantee(tag);
        String prompt = conveyorTagConfig.getRetellingPrompt();
        if (prompt == null) {
            log.error("Не удалось определить retelling prompt по тегу: {}", tag);

            return;
        }

        log.info("Успешно определен prompt по тегу: {}. Для материала: {}", tag, content.getTitle());

        String retellingResponse = openAiClient.promptingCall(processingEvent.getType().name(), prompt, content.getContent());

        Retelling retelling = retellingRepository.save(Retelling.builder()
            .contentId(content.getId())
            .prompt(prompt)
            .aiModel("deepseek-chat")
            .retelling(retellingResponse)
            .build()
        );

        processingEvent.setType(ProcessingEventType.PUBLISH_RETELLING);
        processingEvent.setRetellingId(retelling.getId());
        processingEventDomainService.save(processingEvent);

        log.info("Успешно выполнен пересказ материала по тегу: {}. Название материала: {}", processingEvent.getType().name(), content.getTitle());
    }

    private Content findContent(ProcessingEvent processingEvent) {
        Content content = contentRepository.findById(processingEvent.getContentId())
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
