package ru.panyukovnn.videoretellingbot.tool;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubtitlesLoaderTool {

    private final YtSubtitlesTool ytSubtitlesTool;

    @Tool(description = "Загружает субтитры YouTube-видео по URL")
    public String loadSubtitles(String videoUrl) {
        return ytSubtitlesTool.loadSubtitles(videoUrl);
    }
}
