package ru.panyukovnn.videoretellingbot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ChatModelCallAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.panyukovnn.videoretellingbot.tool.SubtitlesLoaderTool;

@Configuration
public class ChatClientConfig {

    @Bean
    public MessageWindowChatMemory messageWindowChatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .build();
    }

    @Bean
    public ChatClient chatClient(
            ChatModel chatModel,
            MessageWindowChatMemory messageWindowChatMemory,
            SubtitlesLoaderTool subtitlesLoaderTool) {
        return ChatClient.builder(chatModel)
            .defaultAdvisors(
                ChatModelCallAdvisor.builder()
                    .chatModel(chatModel)
                    .build(),
                MessageChatMemoryAdvisor.builder(messageWindowChatMemory)
                    .build()
            )
            .defaultTools(subtitlesLoaderTool)
            .build();
    }
}
