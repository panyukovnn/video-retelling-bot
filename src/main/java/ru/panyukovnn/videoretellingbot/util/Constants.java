package ru.panyukovnn.videoretellingbot.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {

    public static final String DEFAULT_DB_USER = "retelling-bot";
    public static final String PROCESSING_MESSAGE = "Загружаю и обрабатываю видео, подождите...";
    public static final String START_MESSAGE =
        "Привет! Я бот для пересказа YouTube-видео.\n\n"
        + "Как это работает:\n"
        + "1. Пришли мне ссылку на YouTube-видео\n"
        + "2. Я выполню пересказ его содержимого\n"
        + "3. После пересказа можно задавать вопросы по содержанию видео\n"
        + "4. Если прислать новую ссылку — контекст предыдущего видео сбрасывается и начинается обсуждение нового";
    public static final String MULTIPLE_LINKS_WARNING_MESSAGE =
        "Обнаружено несколько ссылок — обработана только первая";
    public static final String FEEDBACK_MESSAGE_TEMPLATE =
        "Если у вас есть пожелания или жалобы — оставьте отзыв: %s";
    public static final Pattern YOUTUBE_VIDEO_ID_PATTERN = Pattern.compile("^[\\w-]{11}$");
    public static final Pattern YOUTUBE_URL_PATTERN = Pattern.compile(
        "https?://(?:(?:www\\.)?youtube\\.com/(?:watch\\?v=|shorts/|live/)|youtu\\.be/)[\\w-]{11}[^\\s]*"
    );
}
