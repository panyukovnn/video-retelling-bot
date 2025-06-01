package ru.panyukovnn.videoretellingbot.serivce.eventprocessor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.panyukovnn.videoretellingbot.client.OpenAiClient;
import ru.panyukovnn.videoretellingbot.exception.ConveyorException;
import ru.panyukovnn.videoretellingbot.exception.InvalidProcessingEventException;
import ru.panyukovnn.videoretellingbot.model.Prompt;
import ru.panyukovnn.videoretellingbot.model.content.Content;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEvent;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEventType;
import ru.panyukovnn.videoretellingbot.serivce.domain.ContentDomainService;
import ru.panyukovnn.videoretellingbot.serivce.domain.ProcessingEventDomainService;
import ru.panyukovnn.videoretellingbot.serivce.domain.PromptDomainService;
import ru.panyukovnn.videoretellingbot.serivce.eventprocessor.EventProcessor;
import ru.panyukovnn.videoretellingbot.util.JsonUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReduceEventProcessorImpl implements EventProcessor {

    private static final int MAX_BATCH_SIZE_KB = 190;

    private final JsonUtil jsonUtil;
    private final OpenAiClient openAiClient;
    private final PromptDomainService promptDomainService;
    private final ContentDomainService contentDomainService;
    private final ProcessingEventDomainService processingEventDomainService;

    @Override
    public void process(ProcessingEvent processingEvent) {
        List<Content> contentsForReducing = contentDomainService.findByParentBatchId(processingEvent.getContentBatchId());
        if (contentsForReducing.isEmpty()) {
            throw new InvalidProcessingEventException("b475", "Не удалось найти промты");
        }

        // TODO можно подстраховаться, если контент больше 8 тысяч токенов, то что-то не так
        // TODO если один результат после маппинга, то нужно сразу возвращать

        Prompt prompt = promptDomainService.findById(processingEvent.getPromptId())
            .orElseThrow(() -> new InvalidProcessingEventException("b475", "Не удалось найти промты"));

        try {

            List<String> contentList = contentsForReducing.stream()
                .map(Content::getContent)
                .toList();

            Content anyContent = contentsForReducing.get(0);

            String reducedResponse = contentsForReducing.size() == 1
                ? anyContent.getContent()
                : reduceTillSingleResult(processingEvent, contentList, prompt);

            Content reducedContent = createReducedContent(anyContent, reducedResponse);

            processingEvent.setType(ProcessingEventType.PUBLISHING);
            processingEvent.setContentId(reducedContent.getId());

            log.info("Успешно выполнен '{}' для batchId: {}", getProcessingEventType(), processingEvent.getContentBatchId());
        } catch (Exception e) {
            log.error("Произошла ошибка на этапе '{}' для batchId: {}. Текст ошибки: {}", getProcessingEventType(), processingEvent.getContentBatchId(), e.getMessage(), e);

            processingEvent.setType(ProcessingEventType.REDUCING_ERROR);
        } finally {
            processingEventDomainService.save(processingEvent);
        }
    }

    @Override
    public ProcessingEventType getProcessingEventType() {
        return ProcessingEventType.REDUCE;
    }

    private String reduceTillSingleResult(ProcessingEvent processingEvent, List<String> contentList, Prompt prompt) {
        int counter = 0;

        List<List<String>> contentBatches = createContentBatches(contentList);
        while (true) {
            List<String> reducedBatchResults = new ArrayList<>();

            for (List<String> contentBatch : contentBatches) {
                String batchAsContent = jsonUtil.toJson(contentBatch);

                String reducesResponse = openAiClient.promptingCall(
                    processingEvent.getType().name(),
                    prompt.getReducePrompt(),
                    batchAsContent);

                reducedBatchResults.add(reducesResponse);
            }

            if (reducedBatchResults.size() == 1) {
                return reducedBatchResults.get(0);
            }
            if (CollectionUtils.isEmpty(contentBatches)) {
                throw new ConveyorException("3901", "Ошибка reduce, не удалось сформировать контент для пересказа");
            }

            log.info("Выполняю '{}' итерацию reduce для контента", counter++);

            contentBatches = createContentBatches(reducedBatchResults);
        }
    }

    private Content createReducedContent(Content anyContent, String reducedResponse) {
        return contentDomainService.save(Content.builder()
            .link(anyContent.getLink())
            .type(anyContent.getType())
            .source(anyContent.getSource())
            .title(anyContent.getTitle())
            .meta(null)
            .publicationDate(anyContent.getPublicationDate())
            .content(reducedResponse)
            .parentBatchId(anyContent.getChildBatchId())
            .childBatchId(null)
            .build());
    }

    private List<List<String>> createContentBatches(List<String> contents) {
        List<List<String>> batches = new ArrayList<>();
        List<String> currentBatch = new ArrayList<>();
        int currentBatchSizeBytes = 0;

        for (String content : contents) {
            try {
                int contentBytesSize = content.getBytes(StandardCharsets.UTF_8).length;

                if (currentBatchSizeBytes + contentBytesSize > MAX_BATCH_SIZE_KB * 1024 && !currentBatch.isEmpty()) {
                    batches.add(new ArrayList<>(currentBatch));
                    currentBatch.clear();
                    currentBatchSizeBytes = 0;
                }

                currentBatch.add(content);
                currentBatchSizeBytes += contentBytesSize;
            } catch (Exception e) {
                log.error("Ошибка при сериализации сообщения в JSON: {}", e.getMessage());
            }
        }

        if (!currentBatch.isEmpty()) {
            batches.add(currentBatch);
        }

        return batches;
    }
}
