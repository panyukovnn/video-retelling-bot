package ru.panyukovnn.videoretellingbot.client;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Service;
import ru.panyukovnn.videoretellingbot.property.PromptProperties;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiClient {

    private final ChatClient chatClient;
    private final MessageWindowChatMemory messageWindowChatMemory;
    private final PromptProperties promptProperties;

    /**
     * Начинает диалог с пересказом видео.
     * Субтитры передаются напрямую в сообщение пользователя, чтобы они всегда были доступны в истории диалога.
     * Если передан userInstruction — добавляется к субтитрам как уточняющий запрос.
     */
    public String startRetelling(String conversationId, String videoUrl, String subtitles, @Nullable String userInstruction) {
        log.info("Начинаю пересказ видео. conversationId: {}", conversationId);

        String subtitlesContext = "Субтитры видео (" + videoUrl + "):\n" + subtitles;
        String userMessage = userInstruction == null
            ? subtitlesContext
            : subtitlesContext + "\n\n" + userInstruction;

        String content = chatClient.prompt()
            .system(promptProperties.getYoutubeRetelling())
            .user(userMessage)
            .advisors(
                MessageChatMemoryAdvisor.builder(messageWindowChatMemory)
                    .conversationId(conversationId)
                    .build()
            )
            .call()
            .content();

        log.info("Пересказ сформирован. conversationId: {}", conversationId);

        return content;
    }

    /**
     * Продолжает существующий диалог, отвечая на вопрос пользователя.
     */
    public String continueDialog(String conversationId, String userMessage) {
        log.info("Продолжаю диалог. conversationId: {}", conversationId);

        String content = chatClient.prompt()
            .user(userMessage)
            .advisors(
                MessageChatMemoryAdvisor.builder(messageWindowChatMemory)
                    .conversationId(conversationId)
                    .build()
            )
            .call()
            .content();

        log.info("Ответ на вопрос сформирован. conversationId: {}", conversationId);

        return content;
    }
}
