package ru.panyukovnn.videoretellingbot.exception;

import lombok.Getter;

@Getter
public class SubtitlesTooLongException extends RuntimeException {

    private final int actualTokens;
    private final int maxTokens;

    public SubtitlesTooLongException(int actualTokens, int maxTokens) {
        super("Субтитры видео превышают бюджет токенов модели: " + actualTokens + " > " + maxTokens);
        this.actualTokens = actualTokens;
        this.maxTokens = maxTokens;
    }
}