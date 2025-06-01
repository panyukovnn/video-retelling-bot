package ru.panyukovnn.videoretellingbot.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BigTextUtils {

    // TODO неэффективный алгоритм
    public static List<String> splitByWords(String text, int maxWordsPerChunk) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        // Регулярка для захвата слов И пробелов/разделителей
        Pattern pattern = Pattern.compile("\\S+|\\s+");
        Matcher matcher = pattern.matcher(text);

        StringBuilder currentChunk = new StringBuilder();
        int wordCount = 0;

        while (matcher.find()) {
            String token = matcher.group();

            if (token.trim().isEmpty()) {
                // Разделитель (пробел, таб, \n и т.п.)
                currentChunk.append(token);
            } else {
                // Слово
                if (wordCount == maxWordsPerChunk) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk.setLength(0);
                    wordCount = 0;
                }
                currentChunk.append(token);
                wordCount++;
            }
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }
}
