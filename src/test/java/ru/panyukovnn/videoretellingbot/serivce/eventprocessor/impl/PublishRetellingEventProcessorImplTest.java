package ru.panyukovnn.videoretellingbot.serivce.eventprocessor.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import ru.panyukovnn.videoretellingbot.serivce.telegram.TgSender;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublishRetellingEventProcessorImplTest {

    @Mock
    private TgSender tgSender;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private RetellingRepository retellingRepository;
    @Mock
    private PublishingProperties publishingProperties;
    @Mock
    private ConveyorTagProperties conveyorTagProperties;
    @Mock
    private ProcessingEventDomainService processingEventDomainService;

    @InjectMocks
    private PublishRetellingEventProcessorImpl publishRetellingEventProcessor;

    @Test
    void when_process_withValidRetelling_then_success() {
        // Arrange
        UUID contentId = UUID.randomUUID();
        UUID retellingId = UUID.randomUUID();
        
        Content content = new Content();
        content.setId(contentId);
        content.setTitle("Test Title");
        content.setLink("https://test.com");

        Retelling retelling = new Retelling();
        retelling.setId(retellingId);
        retelling.setContentId(contentId);
        retelling.setRetelling("Test retelling");

        ProcessingEvent processingEvent = new ProcessingEvent();
        processingEvent.setRetellingId(retellingId);
        processingEvent.setConveyorTag(ConveyorTag.JAVA_HABR);

        ConveyorTagProperties.ConveyorTagConfig conveyorTagConfig = new ConveyorTagProperties.ConveyorTagConfig();
        conveyorTagConfig.setPublishingTopicId(987654321L);

        when(retellingRepository.findById(retellingId)).thenReturn(Optional.of(retelling));
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(publishingProperties.getChatId()).thenReturn(123456789L);
        when(conveyorTagProperties.getWithGuarantee(ConveyorTag.JAVA_HABR)).thenReturn(conveyorTagConfig);

        // Act
        publishRetellingEventProcessor.process(processingEvent);

        // Assert
        verify(retellingRepository).findById(retellingId);
        verify(contentRepository).findById(contentId);
        verify(tgSender).sendMessage(eq(123456789L), eq(987654321L), anyString());
        verify(processingEventDomainService).save(argThat(event ->
            event.getType() == ProcessingEventType.PUBLISHED
        ));
    }

    @Test
    void when_process_withNonExistentRetelling_then_throwException() {
        // Arrange
        UUID retellingId = UUID.randomUUID();
        ProcessingEvent processingEvent = new ProcessingEvent();
        processingEvent.setRetellingId(retellingId);
        processingEvent.setConveyorTag(ConveyorTag.JAVA_HABR);

        when(retellingRepository.findById(retellingId)).thenReturn(Optional.empty());

        // Act
        InvalidProcessingEventException invalidProcessingEventException = assertThrows(
            InvalidProcessingEventException.class,
            () -> publishRetellingEventProcessor.process(processingEvent)
        );

        // Assert
        assertEquals("9f9f", invalidProcessingEventException.getId());
        assertEquals("Не удалось выполнить публикацию пересказа, поскольку не найден пересказ, событие будет удалено", invalidProcessingEventException.getMessage());
        verifyNoMoreInteractions(contentRepository, tgSender);
    }

    @Test
    void when_process_withTgError_then_markAsError() {
        // Arrange
        UUID contentId = UUID.randomUUID();
        UUID retellingId = UUID.randomUUID();
        
        Content content = new Content();
        content.setId(contentId);
        content.setTitle("Test Title");
        content.setLink("https://test.com");

        Retelling retelling = new Retelling();
        retelling.setId(retellingId);
        retelling.setContentId(contentId);
        retelling.setRetelling("Test retelling");

        ProcessingEvent processingEvent = new ProcessingEvent();
        processingEvent.setRetellingId(retellingId);
        processingEvent.setConveyorTag(ConveyorTag.JAVA_HABR);

        ConveyorTagProperties.ConveyorTagConfig conveyorTagConfig = new ConveyorTagProperties.ConveyorTagConfig();
        conveyorTagConfig.setPublishingTopicId(987654321L);

        when(retellingRepository.findById(retellingId)).thenReturn(Optional.of(retelling));
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(publishingProperties.getChatId()).thenReturn(123456789L);
        when(conveyorTagProperties.getWithGuarantee(ConveyorTag.JAVA_HABR)).thenReturn(conveyorTagConfig);
        doThrow(new RuntimeException("Telegram error")).when(tgSender).sendMessage(any(Long.class), any(Long.class), anyString());

        // Act
        publishRetellingEventProcessor.process(processingEvent);

        // Assert
        verify(processingEventDomainService).save(argThat(event ->
            event.getType() == ProcessingEventType.PUBLICATION_ERROR
        ));
    }

    @Test
    void when_getProcessingEventType_then_returnPublishRetelling() {
        // Act
        ProcessingEventType type = publishRetellingEventProcessor.getProcessingEventType();

        // Assert
        assertEquals(ProcessingEventType.PUBLISH_RETELLING, type);
    }
} 