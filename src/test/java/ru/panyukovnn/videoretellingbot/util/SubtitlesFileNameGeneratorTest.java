package ru.panyukovnn.videoretellingbot.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SubtitlesFileNameGeneratorTest {

    @InjectMocks
    private SubtitlesFileNameGenerator generator;

    @Test
    void when_generateFileName_then_success() {
        // Act
        String fileName = generator.generateFileName();

        // Assert
        String dateTimeStr = fileName.substring("subtitles-".length(), fileName.length() - 9); // -9 для UUID
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
        assertNotNull(dateTime);
    }

    @Test
    void when_generateFileName_then_containsValidUUID() {
        // Act
        String fileName = generator.generateFileName();

        // Assert
        String uuidPart = fileName.substring(fileName.length() - 8);
        assertTrue(Pattern.matches("[a-f0-9]{8}", uuidPart));
    }
} 