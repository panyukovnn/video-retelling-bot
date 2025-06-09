package ru.panyukovnn.videoretellingbot.serivce.eventprocessor.impl;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.panyukovnn.videoretellingbot.client.OpenAiClient;
import ru.panyukovnn.videoretellingbot.model.ConveyorTag;
import ru.panyukovnn.videoretellingbot.model.content.Content;
import ru.panyukovnn.videoretellingbot.model.content.ContentType;
import ru.panyukovnn.videoretellingbot.model.content.Source;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEvent;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEventType;
import ru.panyukovnn.videoretellingbot.property.HardcodedPromptProperties;
import ru.panyukovnn.videoretellingbot.serivce.domain.ContentDomainService;
import ru.panyukovnn.videoretellingbot.serivce.domain.ProcessingEventDomainService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetellingEventProcessorImplTest {

    @Mock
    private OpenAiClient openAiClient;
    @Mock
    private ContentDomainService contentDomainService;
    @Mock
    private HardcodedPromptProperties hardcodedPromptProperties;
    @Mock
    private ProcessingEventDomainService processingEventDomainService;

    @InjectMocks
    private RetellingEventProcessorImpl retellingEventProcessor;

    @Test
    void when_process_withValidContent_then_success() {
        // Arrange
        UUID contentId = UUID.randomUUID();
        Content content = new Content();
        content.setId(contentId);
        content.setTitle("Test Title");
        content.setLink("Test Link");
        content.setContent("Test Content");
        content.setType(ContentType.ARTICLE);
        content.setSource(Source.HABR);
        content.setPublicationDate(LocalDateTime.now());
        content.setChildBatchId(UUID.randomUUID());

        ProcessingEvent processingEvent = new ProcessingEvent();
        processingEvent.setContentId(contentId);
        processingEvent.setType(ProcessingEventType.RETELLING);
        processingEvent.setConveyorTag(ConveyorTag.JAVA_HABR);

        when(contentDomainService.findById(contentId)).thenReturn(Optional.of(content));
        when(hardcodedPromptProperties.getJavaHabrRetelling()).thenReturn("Retelling prompt");
        when(openAiClient.promptingCall(anyString(), anyString(), anyString()))
            .thenReturn("Retelling content");
        when(contentDomainService.save(any(Content.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        retellingEventProcessor.process(processingEvent);

        // Assert
        verify(contentDomainService).findById(contentId);
        verify(openAiClient).promptingCall("RETELLING", "Retelling prompt", "Test Content");
        verify(contentDomainService).save(argThat(savedContent ->
            savedContent.getLink().equals(content.getLink()) &&
                savedContent.getType().equals(ContentType.ARTICLE) &&
                savedContent.getSource().equals(Source.HABR) &&
                savedContent.getTitle().equals(content.getTitle()) &&
                savedContent.getMeta() == null &&
                savedContent.getContent().equals("Retelling content") &&
                savedContent.getPublicationDate().equals(content.getPublicationDate()) &&
                savedContent.getParentBatchId().equals(content.getChildBatchId()) &&
                savedContent.getChildBatchId() == null
        ));
        verify(processingEventDomainService).save(argThat(event ->
            event.getType() == ProcessingEventType.PUBLISHING
        ));
    }

    @Test
    void when_process_withNonExistentContent_then_throwException() {
        // Arrange
        UUID contentId = UUID.randomUUID();
        ProcessingEvent processingEvent = new ProcessingEvent();
        processingEvent.setContentId(contentId);
        processingEvent.setConveyorTag(ConveyorTag.JAVA_HABR);

        when(contentDomainService.findById(contentId)).thenReturn(Optional.empty());

        // Act
        assertThrows(EntityNotFoundException.class, () -> retellingEventProcessor.process(processingEvent));

        // Assert
        verify(processingEventDomainService).delete(processingEvent);
        verifyNoMoreInteractions(openAiClient, contentDomainService);
    }

    @Test
    void when_getProcessingEventType_then_returnRetelling() {
        // Act
        ProcessingEventType type = retellingEventProcessor.getProcessingEventType();

        // Assert
        assertEquals(ProcessingEventType.RETELLING, type);
    }
} 