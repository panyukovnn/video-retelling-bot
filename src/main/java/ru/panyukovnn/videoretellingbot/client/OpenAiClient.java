package ru.panyukovnn.videoretellingbot.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiClient {

    private final OpenAiChatModel chatModel;
    private final ExecutorService promptingExecutor;

    public String promptingCall(String requestType, String prompt, String contentToRetell) {
        return blockingCall(requestType, prompt, contentToRetell);
    }

    public CompletableFuture<String> promptingCallAsync(String requestType, String prompt, String contentToRetell) {
        return CompletableFuture.supplyAsync(
            () -> blockingCall(requestType, prompt, contentToRetell),
            promptingExecutor
        );
    }

    private String blockingCall(String requestType, String prompt, String contentToRetell) {
        log.info("Отправляю запрос в AI для: {}", requestType);

        return chatModel.call(new Prompt(prompt + "\n\n" + contentToRetell))
            .getResult()
            .getOutput()
            .getText();
    }
}
