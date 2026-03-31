package ru.panyukovnn.videoretellingbot.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {

    public static final String DEFAULT_DB_USER = "retelling-bot";
    public static final Pattern YOUTUBE_VIDEO_ID_PATTERN = Pattern.compile("^[\\w-]{11}$");
    public static final Pattern YOUTUBE_URL_PATTERN = Pattern.compile(
        "https?://(?:(?:www\\.)?youtube\\.com/(?:watch\\?v=|shorts/|live/)|youtu\\.be/)[\\w-]{11}[^\\s]*"
    );
}
