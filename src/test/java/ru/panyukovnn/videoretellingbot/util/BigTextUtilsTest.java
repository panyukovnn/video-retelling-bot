package ru.panyukovnn.videoretellingbot.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BigTextUtilsTest {

    @Test
    void when_splitByWords_withNullText_then_returnEmptyList() {
        // Act
        List<String> result = BigTextUtils.splitByWords(null, 10);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void when_splitByWords_withEmptyText_then_returnEmptyList() {
        // Act
        List<String> result = BigTextUtils.splitByWords("", 10);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void when_splitByWords_withBlankText_then_returnEmptyList() {
        // Act
        List<String> result = BigTextUtils.splitByWords("   ", 10);

        // Assert
        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
        "'Hello world', 1, 'Hello,world'",
        "'Hello world', 2, 'Hello world'",
        "'Hello world', 3, 'Hello world'",
        "'Hello world! How are you?', 2, 'Hello world!,How are,you?'",
        "'Hello world! How are you?', 3, 'Hello world! How,are you?'",
        "'Hello world! How are you?', 4, 'Hello world! How are,you?'",
        "'Hello world! How are you?', 5, 'Hello world! How are you?'"
    })
    void when_splitByWords_then_splitCorrectly(String text, int maxWordsPerChunk, String expectedChunks) {
        // Act
        List<String> result = BigTextUtils.splitByWords(text, maxWordsPerChunk);

        // Assert
        List<String> expected = List.of(expectedChunks.split(","));
        assertEquals(expected, result);
    }

    @Test
    void when_splitByWords_withMultipleSpaces_then_preserveSpaces() {
        // Arrange
        String text = "Hello   world!   How   are   you?";
        int maxWordsPerChunk = 2;

        // Act
        List<String> result = BigTextUtils.splitByWords(text, maxWordsPerChunk);

        // Assert
        assertEquals(List.of("Hello   world!", "How   are", "you?"), result);
    }

    @Test
    void when_splitByWords_withNewlines_then_preserveNewlines() {
        // Arrange
        String text = "Hello\nworld!\nHow\nare\nyou?";
        int maxWordsPerChunk = 2;

        // Act
        List<String> result = BigTextUtils.splitByWords(text, maxWordsPerChunk);

        // Assert
        assertEquals(List.of("Hello\nworld!", "How\nare", "you?"), result);
    }

    @Test
    void when_splitByWords_withMixedWhitespace_then_preserveWhitespace() {
        // Arrange
        String text = "Hello \t\n world! \t\n How \t\n are \t\n you?";
        int maxWordsPerChunk = 2;

        // Act
        List<String> result = BigTextUtils.splitByWords(text, maxWordsPerChunk);

        // Assert
        assertEquals(List.of("Hello \t\n world!", "How \t\n are", "you?"), result);
    }
} 