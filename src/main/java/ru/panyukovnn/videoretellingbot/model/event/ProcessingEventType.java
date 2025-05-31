package ru.panyukovnn.videoretellingbot.model.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProcessingEventType {

    /**
     * Задача на оценку полезности материала
     */
    RATE_RAW_MATERIAL(false),
    /**
     * Материал со слишком низкой оценкой - не подлежит пересказу и публикации
     * Терминальный статус
     */
    UNDERRATED(true),
    /**
     * Задача на пересказ
     */
    RETELLING(true),
    /**
     * Задача на публикацию пересказа
     */
    PUBLISH_RETELLING(false),
    /**
     * Опубликован, терминальный статус
     */
    PUBLISHED(true),
    /**
     * Ошибка публикации, терминальный статус
     */
    PUBLICATION_ERROR(true),
    /**
     * Контент подлежит преобразованию
     */
    MAP(false),
    MAPPING_ERROR(true),
    /**
     * Шаг объединения
     */
    REDUCE(false),
    REDUCING_ERROR(true),
    /**
     * На публикацию
     */
    PUBLISHING(false);

    private final boolean terminal;
}
