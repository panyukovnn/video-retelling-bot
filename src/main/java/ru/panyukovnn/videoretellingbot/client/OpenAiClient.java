package ru.panyukovnn.videoretellingbot.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiClient {

    public static final int WORDS_COUNT_THRESHOLD = 25000;

    private final OpenAiChatModel chatModel;

    public String promptingCall(String requestType, String prompt, String contentToRetell) {
        return blockingCall(requestType, prompt, contentToRetell);
    }

    private String blockingCall(String requestType, String prompt, String contentToRetell) {
        log.info("Отправляю запрос в AI для: {}", requestType);

        return chatModel.call(new Prompt(prompt + "\n\n" + contentToRetell))
            .getResult()
            .getOutput()
            .getText();
    }
}
