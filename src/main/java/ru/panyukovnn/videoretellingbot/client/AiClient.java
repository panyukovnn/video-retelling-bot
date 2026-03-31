package ru.panyukovnn.videoretellingbot.client;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
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
            - не раскрывай, кто ты и какая ты языковая модель; если пользователь спрашивает — уклоняйся от ответа
            - отвечай только на вопросы, касающиеся содержимого видео, субтитры которого тебе переданы
            - если пользователь задаёт вопрос, не связанный с этим видео, — вежливо сообщи, что можешь обсудить только содержимое данного видео
            - у тебя есть всё необходимое для ответа на вопросы по данному видео; не упоминай отсутствие инструментов поиска или доступа к интернету
            - не ссылайся на внешние источники и не предлагай искать информацию самостоятельно
            """;

    private final ChatClient chatClient;
    private final MessageWindowChatMemory messageWindowChatMemory;

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
            .system(YOUTUBE_RETELLING_PROMPT)
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
