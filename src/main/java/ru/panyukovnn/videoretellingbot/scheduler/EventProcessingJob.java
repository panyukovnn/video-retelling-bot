package ru.panyukovnn.videoretellingbot.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.panyukovnn.videoretellingbot.exception.InvalidProcessingEventException;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEvent;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEventType;
import ru.panyukovnn.videoretellingbot.serivce.domain.ProcessingEventDomainService;
import ru.panyukovnn.videoretellingbot.serivce.eventprocessor.EventProcessor;
import ru.panyukovnn.videoretellingbot.util.JsonUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessingJob {

    public static final List<ProcessingEventType> NON_TERMINAL_PROCESSING_EVENT_TYPES = Arrays.stream(ProcessingEventType.values())
        .filter(Predicate.not(ProcessingEventType::isTerminal))
        .toList();

    private final JsonUtil jsonUtil;
    private final ProcessingEventDomainService processingEventDomainService;
    private final Map<ProcessingEventType, EventProcessor> eventProcessorByType;

    @Async("publicationScheduler")
    @Scheduled(cron = "${retelling.scheduled-jobs.event-processing.cron}")
    public void processEvents() {
        List<ProcessingEvent> events = processingEventDomainService.findAllByTypeIn(NON_TERMINAL_PROCESSING_EVENT_TYPES);

        events.forEach(event -> {
            try {
                eventProcessorByType.get(event.getType())
                    .process(event);
            } catch (InvalidProcessingEventException e) {
                log.error("Некорректный ProcessingEvent: {}. Сообщение: {}", jsonUtil.toJson(event), e.getMessage(), e);

                processingEventDomainService.delete(event);
            } catch (Exception e) {
                log.error("Ошибка обработке события: {}. Сообщение: {}", jsonUtil.toJson(event), e.getMessage(), e);
            }
        });
    }
}
