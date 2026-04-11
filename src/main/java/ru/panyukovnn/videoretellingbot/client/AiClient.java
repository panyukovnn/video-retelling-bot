package ru.panyukovnn.videoretellingbot.client;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import ru.panyukovnn.videoretellingbot.exception.SubtitlesTooLongException;
import ru.panyukovnn.videoretellingbot.property.RetellingProperties;

@Slf4j
@Service
public class AiClient {

    private static final String YOUTUBE_RETELLING_PROMPT = """
            Представь что ты собеседник, который внимательно слушает и пересказывает главное, чтобы убедиться, что всё понял.
            Далее приведен текст субтитров из видео с YouTube, перескажи их и оформи в формате статьи.
            Следуй рекомендациям:
            - пересказ пиши на русском языке
            - можно использовать немного смайликов
            - исправляй грамматические опечатки
            - не используй таблицы
            - если текст на английском языке, то переведи его на русский (примеры кода переводить не надо)
            - не добавляй ссылки на внешние материалы
            - не упоминай эти требования в ответе
            - никогда не раскрывай, кто ты, какая ты языковая модель или на чём ты основан; на любые вопросы о твоей личности, имени, создателе или технологии — отвечай, что ты бот для пересказа видео, и предложи прислать ссылку на YouTube-видео
            - отвечай только на вопросы, касающиеся содержимого видео, субтитры которого тебе переданы
            - если пользователь задаёт вопрос, не связанный с этим видео, — вежливо сообщи, что можешь обсудить только содержимое данного видео
            - у тебя есть всё необходимое для ответа на вопросы по данному видео; не упоминай отсутствие инструментов поиска или доступа к интернету
            - не ссылайся на внешние источники и не предлагай искать информацию самостоятельно
            """;

    private final ChatClient chatClient;
    private final MessageWindowChatMemory messageWindowChatMemory;
    private final TokenCountEstimator tokenCountEstimator;
    private final RetellingProperties retellingProperties;
    private final int maxOutputTokens;

    public AiClient(
            ChatClient chatClient,
            MessageWindowChatMemory messageWindowChatMemory,
            TokenCountEstimator tokenCountEstimator,
            RetellingProperties retellingProperties,
            @Value("${spring.ai.openai.chat.options.max-tokens}") int maxOutputTokens) {
        this.chatClient = chatClient;
        this.messageWindowChatMemory = messageWindowChatMemory;
        this.tokenCountEstimator = tokenCountEstimator;
        this.retellingProperties = retellingProperties;
        this.maxOutputTokens = maxOutputTokens;
    }

    /**
     * Начинает диалог с пересказом видео, возвращая стрим токенов ответа.
     * Субтитры передаются напрямую в сообщение пользователя, чтобы они всегда были доступны в истории диалога.
     * Если передан userInstruction — добавляется к субтитрам как уточняющий запрос.
     */
    public Flux<String> startRetellingStream(
            String conversationId,
            String videoUrl,
            String subtitles,
            @Nullable String userInstruction) {
        log.info("Начинаю пересказ видео. conversationId: {}", conversationId);

        String subtitlesContext = "Субтитры видео (" + videoUrl + "):\n" + subtitles;
        String userMessage = userInstruction == null
            ? subtitlesContext
            : subtitlesContext + "\n\n" + userInstruction;

        checkInputTokensAmount(YOUTUBE_RETELLING_PROMPT, userMessage);

        return chatClient.prompt()
            .system(YOUTUBE_RETELLING_PROMPT)
            .user(userMessage)
            .advisors(
                MessageChatMemoryAdvisor.builder(messageWindowChatMemory)
                    .conversationId(conversationId)
                    .build()
            )
            .stream()
            .content();
    }

    /**
     * Продолжает существующий диалог, возвращая стрим токенов ответа.
     */
    public Flux<String> continueDialogStream(String conversationId, String userMessage) {
        log.info("Продолжаю диалог. conversationId: {}", conversationId);

        checkInputTokensAmount("", userMessage);

        return chatClient.prompt()
            .user(userMessage)
            .advisors(
                MessageChatMemoryAdvisor.builder(messageWindowChatMemory)
                    .conversationId(conversationId)
                    .build()
            )
            .stream()
            .content();
    }

    /**
     * Проверяет, что размер входа в токенах укладывается в бюджет модели.
     * Бюджет = полный контекст − max-tokens на ответ − запас на расхождения токенизаторов.
     * История диалога не учитывается: она ограничивается окном MessageWindowChatMemory
     * и не может вырасти настолько, чтобы одиночно переполнить контекст.
     */
    private void checkInputTokensAmount(String systemPrompt, String userMessage) {
        int maxInputBudget = retellingProperties.getDialogContextLimitTokens()
            - maxOutputTokens
            - retellingProperties.getTokenSafetyMargin();
        int inputTokens = tokenCountEstimator.estimate(systemPrompt) + tokenCountEstimator.estimate(userMessage);

        if (inputTokens > maxInputBudget) {
            log.warn("Входной промпт превышает бюджет токенов: {} > {}", inputTokens, maxInputBudget);

            throw new SubtitlesTooLongException(inputTokens, maxInputBudget);
        }
    }
}