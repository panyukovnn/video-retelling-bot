package ru.panyukovnn.videoretellingbot.serivce.eventprocessor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.panyukovnn.videoretellingbot.client.OpenAiClient;
import ru.panyukovnn.videoretellingbot.exception.InvalidProcessingEventException;
import ru.panyukovnn.videoretellingbot.model.Prompt;
import ru.panyukovnn.videoretellingbot.model.content.Content;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEvent;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEventType;
import ru.panyukovnn.videoretellingbot.serivce.domain.ContentDomainService;
import ru.panyukovnn.videoretellingbot.serivce.domain.ProcessingEventDomainService;
import ru.panyukovnn.videoretellingbot.serivce.domain.PromptDomainService;
import ru.panyukovnn.videoretellingbot.serivce.eventprocessor.EventProcessor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class MapEventProcessorImpl implements EventProcessor {

    private final OpenAiClient openAiClient;
    private final PromptDomainService promptDomainService;
    private final ContentDomainService contentDomainService;
    private final ProcessingEventDomainService processingEventDomainService;

    @Override
    public void process(ProcessingEvent processingEvent) {
        List<Content> batchContents = contentDomainService.findByParentBatchId(processingEvent.getContentBatchId());
        if (CollectionUtils.isEmpty(batchContents)) {
            throw new InvalidProcessingEventException("42d6", "Не удалось найти контент по batchId: " + processingEvent.getContentBatchId());
        }

        Prompt prompt = promptDomainService.findById(processingEvent.getPromptId())
            .orElseThrow(() -> new InvalidProcessingEventException("b475", "Не удалось найти промты"));

        try {
            List<CompletableFuture<Void>> mappingFutures = batchContents.stream()
                .map(content -> openAiClient.promptingCallAsync(
                        processingEvent.getType().name(),
                        prompt.getMapPrompt(),
                        content.getContent())
                    .thenAccept(mappedResponse -> {
                        Content mappedContent = Content.builder()
                            .link(content.getLink())
                            .type(content.getType())
                            .source(content.getSource())
                            .title(content.getTitle())
                            .meta(null)
                            .publicationDate(content.getPublicationDate())
                            .content(mappedResponse)
                            .parentBatchId(content.getChildBatchId())
                            .childBatchId(null)
                            .build();
                        contentDomainService.save(mappedContent);
                    }))
                .toList();

            mappingFutures.forEach(CompletableFuture::join);

            Content anyContent = batchContents.get(0);

            processingEvent.setContentBatchId(anyContent.getChildBatchId());
            processingEvent.setType(ProcessingEventType.REDUCE);

            log.error("Успешно выполнен '{}' для batchId: {}", getProcessingEventType(), processingEvent.getContentBatchId());
        } catch (Exception e) {
            log.error("Произошла ошибка на этапе '{}' для batchId: {}. Текст ошибки: {}", getProcessingEventType(), processingEvent.getContentBatchId(), e.getMessage(), e);

            processingEvent.setType(ProcessingEventType.MAPPING_ERROR);
        } finally {
            processingEventDomainService.save(processingEvent);
        }
    }

    @Override
    public ProcessingEventType getProcessingEventType() {
        return ProcessingEventType.MAP;
    }

}
