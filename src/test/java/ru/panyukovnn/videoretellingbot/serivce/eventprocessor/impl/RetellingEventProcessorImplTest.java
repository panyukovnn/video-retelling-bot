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
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEvent;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEventType;
import ru.panyukovnn.videoretellingbot.model.retelling.Retelling;
import ru.panyukovnn.videoretellingbot.property.ConveyorTagProperties;
import ru.panyukovnn.videoretellingbot.repository.ContentRepository;
import ru.panyukovnn.videoretellingbot.repository.RetellingRepository;
import ru.panyukovnn.videoretellingbot.serivce.domain.ProcessingEventDomainService;

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
    private ContentRepository contentRepository;
    @Mock
    private RetellingRepository retellingRepository;
    @Mock
    private ConveyorTagProperties conveyorTagProperties;
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
        content.setContent("Test Content");

        ProcessingEvent processingEvent = new ProcessingEvent();
        processingEvent.setContentId(contentId);
        processingEvent.setType(ProcessingEventType.RETELLING);
        processingEvent.setConveyorTag(ConveyorTag.JAVA_HABR);

        ConveyorTagProperties.ConveyorTagConfig conveyorTagConfig = new ConveyorTagProperties.ConveyorTagConfig();
        conveyorTagConfig.setRetellingPrompt("Retelling prompt");

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(conveyorTagProperties.getWithGuarantee(ConveyorTag.JAVA_HABR)).thenReturn(conveyorTagConfig);
        when(openAiClient.promptingCall(anyString(), anyString(), anyString()))
            .thenReturn("Retelling content");
        when(retellingRepository.save(any(Retelling.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        retellingEventProcessor.process(processingEvent);

        // Assert
        verify(contentRepository).findById(contentId);
        verify(openAiClient).promptingCall("RETELLING", "Retelling prompt", "Test Content");
        verify(retellingRepository).save(argThat(retelling ->
            retelling.getContentId().equals(contentId) &&
            retelling.getPrompt().equals("Retelling prompt") &&
            retelling.getAiModel().equals("deepseek-chat") &&
            retelling.getRetelling().equals("Retelling content")
        ));
        verify(processingEventDomainService).save(argThat(event ->
            event.getType() == ProcessingEventType.PUBLISH_RETELLING
        ));
    }

    @Test
    void when_process_withNonExistentContent_then_throwException() {
        // Arrange
        UUID contentId = UUID.randomUUID();
        ProcessingEvent processingEvent = new ProcessingEvent();
        processingEvent.setContentId(contentId);
        processingEvent.setConveyorTag(ConveyorTag.JAVA_HABR);

        when(contentRepository.findById(contentId)).thenReturn(Optional.empty());

        // Act
        assertThrows(EntityNotFoundException.class, () -> retellingEventProcessor.process(processingEvent));

        // Assert
        verify(processingEventDomainService).delete(processingEvent);
        verifyNoMoreInteractions(openAiClient, retellingRepository);
    }

    @Test
    void when_getProcessingEventType_then_returnRetelling() {
        // Act
        ProcessingEventType type = retellingEventProcessor.getProcessingEventType();

        // Assert
        assertEquals(ProcessingEventType.RETELLING, type);
    }
} 