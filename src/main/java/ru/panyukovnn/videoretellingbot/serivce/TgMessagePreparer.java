package ru.panyukovnn.videoretellingbot.serivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static ru.panyukovnn.videoretellingbot.util.Constants.MAX_TG_MESSAGE_SIZE;

@Slf4j
@Service
@RequiredArgsConstructor
public class TgMessagePreparer {

    public List<String> prepareTgMessage(String content) {
        String preparedMessage = changeHashSignsToBoldInMarkdown(content);
//        preparedMessage = escapeSymoblsForMarkdownV2(preparedMessage);
        preparedMessage = replaceDoubleStars(preparedMessage);

        return splitTooLongMessage(preparedMessage);
    }

    protected List<String> splitTooLongMessage(String message) {
        List<String> tgMessages = new ArrayList<>();

        String[] lines = message.split("\n", -1); // -1 сохраняет пустые строки

        StringBuilder currentChunk = new StringBuilder();

        for (String line : lines) {
            String lineWithNewline = line + "\n";

            if (currentChunk.length() + lineWithNewline.length() > MAX_TG_MESSAGE_SIZE) {
                tgMessages.add(currentChunk.toString());
                currentChunk.setLength(0);
            }

            currentChunk.append(lineWithNewline);
        }

        if (!currentChunk.isEmpty()) {
            tgMessages.add(currentChunk.toString());
        }

        return tgMessages;
    }

    protected String changeHashSignsToBoldInMarkdown(String input) {
        String[] lines = input.split("\n", -1); // -1 сохраняет пустые строки

        StringBuilder output = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("#")) {
                String cleaned = line.replaceFirst("^#+", "").trim();
                output
                    .append("*")
                    .append(cleaned)
                    .append("*");
            } else {
                output.append(line);
            }

            output.append("\n");
        }

        return output.toString();
    }

    @Deprecated
    protected String escapeSymbolsForMarkdownV2(String message) {
        return message.replaceAll("([_\\[\\]\\(\\)~>#+\\-=|{}.!])", "\\\\$1");
    }

    /**
     * Заменяем двойные звездочки на одинарные, чтобы корректно отображалось выделение жирным
     */
    private static String replaceDoubleStars(String escapedMessage) {
        return escapedMessage.replaceAll("\\*\\*", "*");
    }
}
