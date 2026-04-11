package ru.panyukovnn.videoretellingbot.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import ru.panyukovnn.videoretellingbot.exception.SubtitlesTooLongException;
import ru.panyukovnn.videoretellingbot.property.RetellingProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiClientUnitTest {

    private static final int MAX_OUTPUT_TOKENS = 8000;
    private static final int DIALOG_CONTEXT_LIMIT_TOKENS = 64000;
    private static final int TOKEN_SAFETY_MARGIN = 512;

    private final ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    private final MessageWindowChatMemory messageWindowChatMemory = mock(MessageWindowChatMemory.class);
    private final TokenCountEstimator tokenCountEstimator = mock(TokenCountEstimator.class);
    private final RetellingProperties retellingProperties = buildProperties();

    private final AiClient aiClient = new AiClient(
        chatClient,
        messageWindowChatMemory,
        tokenCountEstimator,
        retellingProperties,
        MAX_OUTPUT_TOKENS);

    @Test
    void when_startRetellingStream_withInputWithinBudget_then_chatClientCalled() {
        String conversationId = "conv-1";
        String videoUrl = "https://www.youtube.com/watch?v=abc";
        String subtitles = "Короткие субтитры";

        when(tokenCountEstimator.estimate(anyString())).thenReturn(100);

        aiClient.startRetellingStream(conversationId, videoUrl, subtitles, null);

        verify(chatClient).prompt();
    }

    @Test
    void when_startRetellingStream_withInputExceedingBudget_then_throwsAndChatClientNotCalled() {
        String conversationId = "conv-1";
        String videoUrl = "https://www.youtube.com/watch?v=abc";
        String subtitles = "Очень длинные субтитры";

        when(tokenCountEstimator.estimate(anyString())).thenReturn(DIALOG_CONTEXT_LIMIT_TOKENS);

        SubtitlesTooLongException exception = assertThrows(
            SubtitlesTooLongException.class,
            () -> aiClient.startRetellingStream(conversationId, videoUrl, subtitles, null)
        );

        int expectedMaxInputBudget = DIALOG_CONTEXT_LIMIT_TOKENS - MAX_OUTPUT_TOKENS - TOKEN_SAFETY_MARGIN;
        assertEquals(expectedMaxInputBudget, exception.getMaxTokens());
        verifyNoInteractions(chatClient);
    }

    @Test
    void when_continueDialogStream_withInputWithinBudget_then_chatClientCalled() {
        String conversationId = "conv-1";
        String userMessage = "Что главное в видео?";

        when(tokenCountEstimator.estimate(anyString())).thenReturn(50);

        aiClient.continueDialogStream(conversationId, userMessage);

        verify(chatClient).prompt();
    }

    @Test
    void when_continueDialogStream_withInputExceedingBudget_then_throwsAndChatClientNotCalled() {
        String conversationId = "conv-1";
        String userMessage = "Очень длинный вопрос";

        when(tokenCountEstimator.estimate(anyString())).thenReturn(DIALOG_CONTEXT_LIMIT_TOKENS);

        assertThrows(
            SubtitlesTooLongException.class,
            () -> aiClient.continueDialogStream(conversationId, userMessage)
        );

        verifyNoInteractions(chatClient);
    }

    private static RetellingProperties buildProperties() {
        RetellingProperties properties = new RetellingProperties();
        properties.setDialogContextLimitTokens(DIALOG_CONTEXT_LIMIT_TOKENS);
        properties.setTokenSafetyMargin(TOKEN_SAFETY_MARGIN);

        return properties;
    }
}