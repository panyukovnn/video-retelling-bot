package ru.panyukovnn.videoretellingbot.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiClient {

    private final ChatClient chatClient;

    public String promptingCall(String requestType, String prompt, String contentToRetell) {
        log.info("Отправляю запрос в AI для: {}", requestType);

        String content = chatClient.prompt()
            .system(prompt)
            .user(contentToRetell)
            .call()
            .content();

        log.info("Ответ LLM: {}", content);

        return content;
    }
}
