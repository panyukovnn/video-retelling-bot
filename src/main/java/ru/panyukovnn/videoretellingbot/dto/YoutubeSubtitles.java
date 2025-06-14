package ru.panyukovnn.videoretellingbot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class YoutubeSubtitles {

    private String link;
    private String title;
    private Lang lang;
    private String subtitles;
}
