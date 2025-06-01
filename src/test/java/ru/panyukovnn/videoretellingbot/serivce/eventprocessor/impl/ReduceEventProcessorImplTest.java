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
import ru.panyukovnn.videoretellingbot.util.JsonUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReduceEventProcessorImplTest {

    @Mock
    private JsonUtil jsonUtil;
    @Mock
    private OpenAiClient openAiClient;
    @Mock
    private PromptDomainService promptDomainService;
    @Mock
    private ContentDomainService contentDomainService;
    @Mock
    private ProcessingEventDomainService processingEventDomainService;

    @InjectMocks
    private ReduceEventProcessorImpl reduceEventProcessor;

    @Test
    void when_process_withSingleContent_then_success() {
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
            .reducePrompt("Test reduce prompt")
            .build();

        ProcessingEvent processingEvent = new ProcessingEvent();
        processingEvent.setContentBatchId(batchId);
        processingEvent.setPromptId(promptId);
        processingEvent.setType(ProcessingEventType.REDUCE);

        when(contentDomainService.findByParentBatchId(batchId)).thenReturn(List.of(content));
        when(promptDomainService.findById(promptId)).thenReturn(Optional.of(prompt));

        Content reducedContent = Content.builder()
            .id(UUID.randomUUID())
            .build();
        when(contentDomainService.save(any(Content.class))).thenReturn(reducedContent);

        // Act
        reduceEventProcessor.process(processingEvent);

        // Assert
        verify(contentDomainService).findByParentBatchId(batchId);
        verify(promptDomainService).findById(promptId);
        verify(contentDomainService).save(argThat(savedContent ->
            savedContent.getLink().equals(content.getLink()) &&
            savedContent.getType().equals(content.getType()) &&
            savedContent.getSource().equals(content.getSource()) &&
            savedContent.getTitle().equals(content.getTitle()) &&
            savedContent.getContent().equals("Test Content") &&
            savedContent.getParentBatchId().equals(childBatchId) &&
            savedContent.getChildBatchId() == null
        ));
        verify(processingEventDomainService).save(processingEvent);
        verifyNoInteractions(openAiClient);

        assertEquals(ProcessingEventType.PUBLISHING, processingEvent.getType());
        assertEquals(reducedContent.getId(), processingEvent.getContentId());
    }

    @Test
    void when_process_withMultipleContents_then_success() {
        // Arrange
        UUID batchId = UUID.randomUUID();
        UUID promptId = UUID.randomUUID();
        UUID childBatchId = UUID.randomUUID();

        Content content1 = Content.builder()
            .id(UUID.randomUUID())
            .link("https://test.com")
            .type(ContentType.TG_MESSAGE_BATCH)
            .source(Source.TG)
            .title("Test Title 1")
            .content("Test Content 1")
            .publicationDate(LocalDateTime.now())
            .parentBatchId(batchId)
            .childBatchId(childBatchId)
            .build();

        Content content2 = Content.builder()
            .id(UUID.randomUUID())
            .link("https://test.com")
            .type(ContentType.TG_MESSAGE_BATCH)
            .source(Source.TG)
            .title("Test Title 2")
            .content("Test Content 2")
            .publicationDate(LocalDateTime.now())
            .parentBatchId(batchId)
            .childBatchId(childBatchId)
            .build();

        Prompt prompt = Prompt.builder()
            .id(promptId)
            .reducePrompt("Test reduce prompt")
            .build();

        ProcessingEvent processingEvent = new ProcessingEvent();
        processingEvent.setContentBatchId(batchId);
        processingEvent.setPromptId(promptId);
        processingEvent.setType(ProcessingEventType.REDUCE);

        when(contentDomainService.findByParentBatchId(batchId)).thenReturn(List.of(content1, content2));
        when(promptDomainService.findById(promptId)).thenReturn(Optional.of(prompt));
        when(jsonUtil.toJson(any())).thenReturn("[\"Test Content 1\",\"Test Content 2\"]");
        when(openAiClient.promptingCall(anyString(), anyString(), anyString()))
            .thenReturn("Reduced content");

        Content reducedContent = Content.builder()
            .id(UUID.randomUUID())
            .build();
        when(contentDomainService.save(any(Content.class))).thenReturn(reducedContent);

        // Act
        reduceEventProcessor.process(processingEvent);

        // Assert
        verify(contentDomainService).findByParentBatchId(batchId);
        verify(promptDomainService).findById(promptId);
        verify(openAiClient).promptingCall("REDUCE", "Test reduce prompt", "[\"Test Content 1\",\"Test Content 2\"]");
        verify(contentDomainService).save(argThat(savedContent ->
            savedContent.getLink().equals(content1.getLink()) &&
            savedContent.getType().equals(content1.getType()) &&
            savedContent.getSource().equals(content1.getSource()) &&
            savedContent.getTitle().equals(content1.getTitle()) &&
            savedContent.getContent().equals("Reduced content") &&
            savedContent.getParentBatchId().equals(childBatchId) &&
            savedContent.getChildBatchId() == null
        ));
        verify(processingEventDomainService).save(argThat(event ->
            event.getType() == ProcessingEventType.PUBLISHING &&
            event.getContentId() != null
        ));

        assertEquals(ProcessingEventType.PUBLISHING, processingEvent.getType());
        assertEquals(reducedContent.getId(), processingEvent.getContentId());
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
            () -> reduceEventProcessor.process(processingEvent)
        );

        assertEquals("b475", exception.getId());
        assertEquals("Не удалось найти промты", exception.getMessage());
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
            () -> reduceEventProcessor.process(processingEvent)
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
        UUID childBatchId = UUID.randomUUID();

        Content content1 = Content.builder()
            .id(UUID.randomUUID())
            .parentBatchId(batchId)
            .childBatchId(childBatchId)
            .content("Test content 1")
            .build();

        Content content2 = Content.builder()
            .id(UUID.randomUUID())
            .parentBatchId(batchId)
            .childBatchId(childBatchId)
            .content("Test content 2")
            .build();

        Prompt prompt = Prompt.builder()
            .id(promptId)
            .reducePrompt("Test reduce prompt")
            .build();

        ProcessingEvent processingEvent = new ProcessingEvent();
        processingEvent.setContentBatchId(batchId);
        processingEvent.setPromptId(promptId);
        processingEvent.setType(ProcessingEventType.REDUCE);

        when(contentDomainService.findByParentBatchId(batchId)).thenReturn(List.of(content1, content2));
        when(promptDomainService.findById(promptId)).thenReturn(Optional.of(prompt));
        when(openAiClient.promptingCall(anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("OpenAI error"));

        // Act
        reduceEventProcessor.process(processingEvent);

        // Assert
        verify(processingEventDomainService).save(argThat(event ->
            event.getType() == ProcessingEventType.REDUCING_ERROR
        ));
    }

    @Test
    void when_getProcessingEventType_then_returnReduce() {
        // Act
        ProcessingEventType type = reduceEventProcessor.getProcessingEventType();

        // Assert
        assertEquals(ProcessingEventType.REDUCE, type);
    }
} 