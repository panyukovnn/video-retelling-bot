package ru.panyukovnn.videoretellingbot.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.panyukovnn.videoretellingbot.exception.RetellingException;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class YtDlpProcessBuilderCreatorTest {

    @InjectMocks
    private YtDlpProcessBuilderCreator creator;

    @Test
    void when_createProcessBuilder_withAutoSubs_then_success() {
        // Arrange
        String videoUrl = "https://youtube.com/watch?v=test";
        String lang = "ru";
        Boolean isAutoSubs = true;
        String outputFileName = "test.vtt";

        // Act
        var processBuilder = creator.createProcessBuilder(videoUrl, lang, isAutoSubs, outputFileName);

        // Assert
        assertNotNull(processBuilder);
        List<String> command = processBuilder.command();
        assertEquals("./yt-dlp/" + getExpectedExecutableName(), command.get(0));
        assertEquals("--write-auto-subs", command.get(1));
        assertEquals("--sub-lang", command.get(2));
        assertEquals(lang, command.get(3));
        assertEquals("--sub-format", command.get(4));
        assertEquals("vtt", command.get(5));
        assertEquals("--skip-download", command.get(6));
        assertEquals("-o", command.get(7));
        assertEquals(outputFileName, command.get(8));
        assertEquals(videoUrl, command.get(9));
        assertEquals(new File("."), processBuilder.directory());
        assertTrue(processBuilder.redirectErrorStream());
    }

    @Test
    void when_createProcessBuilder_withManualSubs_then_success() {
        // Arrange
        String videoUrl = "https://youtube.com/watch?v=test";
        String lang = "ru";
        Boolean isAutoSubs = false;
        String outputFileName = "test.vtt";

        // Act
        var processBuilder = creator.createProcessBuilder(videoUrl, lang, isAutoSubs, outputFileName);

        // Assert
        assertNotNull(processBuilder);
        List<String> command = processBuilder.command();
        assertEquals("./yt-dlp/" + getExpectedExecutableName(), command.get(0));
        assertEquals("--write-subs", command.get(1));
        assertEquals("--sub-lang", command.get(2));
        assertEquals(lang, command.get(3));
        assertEquals("--sub-format", command.get(4));
        assertEquals("vtt", command.get(5));
        assertEquals("--skip-download", command.get(6));
        assertEquals("-o", command.get(7));
        assertEquals(outputFileName, command.get(8));
        assertEquals(videoUrl, command.get(9));
        assertEquals(new File("."), processBuilder.directory());
        assertTrue(processBuilder.redirectErrorStream());
    }

    private String getExpectedExecutableName() {
        String osName = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        if (osName.contains("mac")) {
            return "yt-dlp_macos";
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return arch.contains("aarch64") || arch.contains("arm64")
                ? "yt-dlp_linux_aarch64"
                : "yt-dlp_linux";
        }

        throw new RetellingException("4824", "Не удалось определить подходящий yt-dlp исполняемый файл для системы: " + osName);
    }
} 