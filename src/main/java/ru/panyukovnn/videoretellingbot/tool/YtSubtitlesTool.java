package ru.panyukovnn.videoretellingbot.tool;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class YtSubtitlesTool {

    @Value("${retelling.yt-subtitles-loader-jar-path}")
    private String ytSubtitlesLoaderJarPath;

    @Tool(description = "Load subtitles of Youtube video by link")
    String getYoutubeVideoSubtitles(String youtubeVideoUrl) {
        return loadSubtitles(youtubeVideoUrl);
    }

    @SneakyThrows
    public String loadSubtitles(String videoUrl) {
        log.info("Начинаю загрузку субтитров по ссылке: {}", videoUrl);

        ProcessBuilder pb = new ProcessBuilder();
        pb.command().add("java");
        pb.command().add("-jar");

        pb.command().add(ytSubtitlesLoaderJarPath);
        pb.command().add(videoUrl);

        // Объединяем stderr в stdout, чтобы всё читать из одного потока
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Возникла ошибка при извлечении субтитров из видео. videoUrl: %s. exitCode: %s. Текст: %s".formatted(videoUrl, exitCode, output));
        }

        log.info("Субтитры по ссылке успешно загружены: {}", videoUrl);

        return output.toString();
    }
}