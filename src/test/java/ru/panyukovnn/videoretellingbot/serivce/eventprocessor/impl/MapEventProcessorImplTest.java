package ru.panyukovnn.videoretellingbot.serivce.eventprocessor.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.panyukovnn.videoretellingbot.client.OpenAiClient;
import ru.panyukovnn.videoretellingbot.exception.InvalidProcessingEventException;
import ru.panyukovnn.videoretellingbot.model.Prompt;
import ru.panyukovnn.videoretellingbot.model.content.Content;
import ru.panyukovnn.videoretellingbot.model.content.ContentType;
import ru.panyukovnn.videoretellingbot.model.content.Source;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEvent;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEventType;
import ru.panyukovnn.videoretellingbot.serivce.domain.ContentDomainService;
import ru.panyukovnn.videoretellingbot.serivce.domain.ProcessingEventDomainService;
import ru.panyukovnn.videoretellingbot.serivce.domain.PromptDomainService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapEventProcessorImplTest {

    @Mock
    private OpenAiClient openAiClient;
    @Mock
    private PromptDomainService promptDomainService;
    @Mock
    private ContentDomainService contentDomainService;
    @Mock
    private ProcessingEventDomainService processingEventDomainService;

    @InjectMocks
    private MapEventProcessorImpl mapEventProcessor;

    @Test
    void when_process_withValidContent_then_success() {
        // Arrange
        UUID batchId = UUID.randomUUID();
        UUID promptId = UUID.randomUUID();
        UUID childBatchId = UUID.randomUUID();

        Content content = Content.builder()
            .id(UUID.randomUUID())
            .link("https://test.com")
            .type(ContentType.TG_MESSAGE_BATCH)
            .source(Source.TG)
            .title("Test Title")
            .content("Test Content")
            .publicationDate(LocalDateTime.now())
            .parentBatchId(batchId)
            .childBatchId(childBatchId)
            .build();

        Prompt prompt = Prompt.builder()
            .id(promptId)
            .mapPrompt("Test map prompt")
            .build();

        ProcessingEvent processingEvent = new ProcessingEvent();
        processingEvent.setContentBatchId(batchId);
        processingEvent.setPromptId(promptId);
        processingEvent.setType(ProcessingEventType.MAP);

        when(contentDomainService.findByParentBatchId(batchId)).thenReturn(List.of(content));
        when(promptDomainService.findById(promptId)).thenReturn(Optional.of(prompt));
        when(openAiClient.promptingCallAsync(anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture("Mapped content"));
        when(contentDomainService.save(any(Content.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        mapEventProcessor.process(processingEvent);

        // Assert
        verify(contentDomainService).findByParentBatchId(batchId);
        verify(promptDomainService).findById(promptId);
        verify(openAiClient).promptingCallAsync("MAP", "Test map prompt", "Test Content");
        verify(contentDomainService).save(argThat(savedContent ->
            savedContent.getLink().equals(content.getLink()) &&
            savedContent.getType().equals(content.getType()) &&
            savedContent.getSource().equals(content.getSource()) &&
            savedContent.getTitle().equals(content.getTitle()) &&
            savedContent.getContent().equals("Mapped content") &&
            savedContent.getParentBatchId().equals(childBatchId) &&
            savedContent.getChildBatchId() == null
        ));
        verify(processingEventDomainService).save(argThat(event ->
            event.getType() == ProcessingEventType.REDUCE &&
            event.getContentBatchId().equals(childBatchId)
        ));
    }

    @Test
    void when_process_withEmptyBatch_then_throwException() {
        // Arrange
        UUID batchId = UUID.randomUUID();
        ProcessingEvent processingEvent = new ProcessingEvent();
        processingEvent.setContentBatchId(batchId);

        when(contentDomainService.findByParentBatchId(batchId)).thenReturn(List.of());

        // Act & Assert
        InvalidProcessingEventException exception = assertThrows(
            InvalidProcessingEventException.class,
            () -> mapEventProcessor.process(processingEvent)
        );

        assertEquals("42d6", exception.getId());
        assertEquals("Не удалось найти контент по batchId: " + batchId, exception.getMessage());
        verifyNoMoreInteractions(promptDomainService, openAiClient);
    }

    @Test
    void when_process_withNonExistentPrompt_then_throwException() {
        // Arrange
        UUID batchId = UUID.randomUUID();
        UUID promptId = UUID.randomUUID();

        Content content = Content.builder()
            .id(UUID.randomUUID())
            .parentBatchId(batchId)
            .build();

        ProcessingEvent processingEvent = new ProcessingEvent();
        processingEvent.setContentBatchId(batchId);
        processingEvent.setPromptId(promptId);

        when(contentDomainService.findByParentBatchId(batchId)).thenReturn(List.of(content));
        when(promptDomainService.findById(promptId)).thenReturn(Optional.empty());

        // Act & Assert
        InvalidProcessingEventException exception = assertThrows(
            InvalidProcessingEventException.class,
            () -> mapEventProcessor.process(processingEvent)
        );

        assertEquals("b475", exception.getId());
        assertEquals("Не удалось найти промты", exception.getMessage());
        verifyNoMoreInteractions(openAiClient);
    }

    @Test
    void when_process_withOpenAiError_then_markAsError() {
        // Arrange
        UUID batchId = UUID.randomUUID();
        UUID promptId = UUID.randomUUID();

        Content content = Content.builder()
            .id(UUID.randomUUID())
            .parentBatchId(batchId)
            .build();

        Prompt prompt = Prompt.builder()
            .id(promptId)
            .mapPrompt("Test map prompt")
            .build();

        ProcessingEvent processingEvent = new ProcessingEvent();
        processingEvent.setContentBatchId(batchId);
        processingEvent.setPromptId(promptId);
        processingEvent.setType(ProcessingEventType.MAP);

        when(contentDomainService.findByParentBatchId(batchId)).thenReturn(List.of(content));
        when(promptDomainService.findById(promptId)).thenReturn(Optional.of(prompt));
        when(openAiClient.promptingCallAsync(anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("OpenAI error")));

        // Act
        mapEventProcessor.process(processingEvent);

        // Assert
        verify(processingEventDomainService).save(argThat(event ->
            event.getType() == ProcessingEventType.MAPPING_ERROR
        ));
    }

    @Test
    void when_getProcessingEventType_then_returnMap() {
        // Act
        ProcessingEventType type = mapEventProcessor.getProcessingEventType();

        // Assert
        assertEquals(ProcessingEventType.MAP, type);
    }
} 