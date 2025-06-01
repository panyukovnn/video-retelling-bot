package ru.panyukovnn.videoretellingbot.serivce.loader.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.panyukovnn.videoretellingbot.exception.RetellingException;
import ru.panyukovnn.videoretellingbot.model.content.Content;
import ru.panyukovnn.videoretellingbot.model.content.ContentType;
import ru.panyukovnn.videoretellingbot.model.content.Lang;
import ru.panyukovnn.videoretellingbot.model.content.Source;
import ru.panyukovnn.videoretellingbot.util.SubtitlesFileNameGenerator;
import ru.panyukovnn.videoretellingbot.util.YtDlpProcessBuilderCreator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YoutubeSubtitlesLoaderTest {

    @Mock
    private YtDlpProcessBuilderCreator ytDlpProcessBuilderCreator;

    @Mock
    private SubtitlesFileNameGenerator subtitlesFileNameGenerator;

    @InjectMocks
    private YoutubeSubtitlesLoader loader;

    private static final String SUBTITLES_DIRECTORY = "./subtitles/";
    private Path subtitlesDir;
    private Path subtitlesFile;

    @BeforeEach
    void setUp() throws IOException {
        // Создаем директорию для субтитров
        subtitlesDir = Path.of(SUBTITLES_DIRECTORY);
        if (!Files.exists(subtitlesDir)) {
            Files.createDirectories(subtitlesDir);
        }

        // Создаем тестовый файл субтитров
        subtitlesFile = subtitlesDir.resolve("test-subtitles.ru.vtt");
        List<String> subtitles = Arrays.asList(
            "WEBVTT",
            "",
            "00:00:01.000 --> 00:00:04.000",
            "<v Speaker>Привет, это тестовые субтитры",
            "",
            "00:00:05.000 --> 00:00:08.000",
            "Это <b>вторая</b> строка субтитров",
            "",
            "00:00:09.000 --> 00:00:12.000",
            "align:start position:0%",
            "И третья строка"
        );
        Files.write(subtitlesFile, subtitles);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Удаляем тестовые файлы
        if (Files.exists(subtitlesFile)) {
            Files.delete(subtitlesFile);
        }
        if (Files.exists(subtitlesDir)) {
            Files.delete(subtitlesDir);
        }
    }

    @Test
    void when_load_withValidSubtitles_then_success() throws Exception {
        // Arrange
        String videoUrl = "https://youtube.com/watch?v=test123";
        String baseFileName = "test-subtitles";
        String fileName = baseFileName + ".ru.vtt";
        ProcessBuilder processBuilderSuccess = mock(ProcessBuilder.class);
        ProcessBuilder processBuilderNoSubtitles = mock(ProcessBuilder.class);
        Process process = mock(Process.class);
        Process processNoSubtitles = mock(Process.class);

        when(subtitlesFileNameGenerator.generateFileName()).thenReturn(baseFileName);
        when(ytDlpProcessBuilderCreator.createProcessBuilder(eq(videoUrl), eq("ru"), eq(false), anyString()))
            .thenReturn(processBuilderSuccess);
        when(ytDlpProcessBuilderCreator.createProcessBuilder(eq(videoUrl), eq("ru"), eq(true), anyString()))
            .thenReturn(processBuilderNoSubtitles);
        when(ytDlpProcessBuilderCreator.createProcessBuilder(eq(videoUrl), eq("en"), eq(false), anyString()))
            .thenReturn(processBuilderNoSubtitles);
        when(ytDlpProcessBuilderCreator.createProcessBuilder(eq(videoUrl), eq("en"), eq(true), anyString()))
            .thenReturn(processBuilderNoSubtitles);
        when(processBuilderSuccess.start()).thenReturn(process);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(process.waitFor()).thenReturn(0);

        when(processBuilderNoSubtitles.start()).thenReturn(processNoSubtitles);
        when(processNoSubtitles.getInputStream()).thenReturn(new ByteArrayInputStream("There are no subtitles for the requested languages".getBytes()));
        when(processNoSubtitles.waitFor()).thenReturn(0);

        // Создаем файл с тем же именем, которое будет использоваться в YoutubeSubtitlesLoader
        Path expectedFile = subtitlesDir.resolve(fileName);
        Files.copy(subtitlesFile, expectedFile);

        try {
            // Act
            Content content = loader.load(videoUrl);

            // Assert
            assertAll(
                () -> assertEquals(videoUrl, content.getLink()),
                () -> assertEquals(ContentType.SUBTITLES, content.getType()),
                () -> assertEquals(Source.YOUTUBE, content.getSource()),
                () -> assertEquals(Lang.RU, content.getLang()),
                () -> assertTrue(content.getContent().contains("Привет, это тестовые субтитры")),
                () -> assertTrue(content.getContent().contains("Это вторая строка субтитров")),
                () -> assertTrue(content.getContent().contains("И третья строка")),
                () -> assertFalse(content.getContent().contains("<")),
                () -> assertFalse(content.getContent().contains("00:00:01.000")),
                () -> assertFalse(content.getContent().contains("align:start"))
            );
        } finally {
            // Очищаем созданный файл
            if (Files.exists(expectedFile)) {
                Files.delete(expectedFile);
            }
        }
    }

    @Test
    void when_load_withNoSubtitles_then_throwException() throws Exception {
        // Arrange
        String videoUrl = "https://youtube.com/watch?v=test123";
        String baseFileName = "test-subtitles";
        ProcessBuilder processBuilder = mock(ProcessBuilder.class);
        Process process = mock(Process.class);

        when(subtitlesFileNameGenerator.generateFileName()).thenReturn(baseFileName);
        when(ytDlpProcessBuilderCreator.createProcessBuilder(any(), any(), anyBoolean(), anyString()))
            .thenReturn(processBuilder);
        when(processBuilder.start()).thenReturn(process);
        when(process.getInputStream()).thenReturn(
            new ByteArrayInputStream("There are no subtitles for the requested languages".getBytes()),
            new ByteArrayInputStream("There are no subtitles for the requested languages".getBytes()),
            new ByteArrayInputStream("There are no subtitles for the requested languages".getBytes()),
            new ByteArrayInputStream("There are no subtitles for the requested languages".getBytes())
        );

        // Act & Assert
        RetellingException exception = assertThrows(
            RetellingException.class,
            () -> loader.load(videoUrl)
        );
        
        assertAll(
            () -> assertEquals("48ae", exception.getId()),
            () -> assertEquals("Не удалось загрузить субтитры для указанного видео", exception.getMessage())
        );
    }

    @Test
    void when_load_withProcessError_then_throwException() throws Exception {
        // Arrange
        String videoUrl = "https://youtube.com/watch?v=test123";
        String baseFileName = "test-subtitles";
        ProcessBuilder processBuilder = mock(ProcessBuilder.class);
        Process process = mock(Process.class);

        when(subtitlesFileNameGenerator.generateFileName()).thenReturn(baseFileName);
        when(ytDlpProcessBuilderCreator.createProcessBuilder(any(), any(), anyBoolean(), anyString()))
            .thenReturn(processBuilder);
        when(processBuilder.start()).thenReturn(process);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(process.waitFor()).thenReturn(1);

        // Act & Assert
        RetellingException exception = assertThrows(
            RetellingException.class,
            () -> loader.load(videoUrl)
        );
        
        assertAll(
            () -> assertEquals("48ae", exception.getId()),
            () -> assertEquals("Не удалось загрузить субтитры для указанного видео", exception.getMessage())
        );
    }

    @Test
    void when_getSource_then_returnYoutube() {
        // Act
        Source source = loader.getSource();

        // Assert
        assertEquals(Source.YOUTUBE, source);
    }
} 