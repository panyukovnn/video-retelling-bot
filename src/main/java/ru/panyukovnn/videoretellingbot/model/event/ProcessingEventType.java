package ru.panyukovnn.videoretellingbot.model.event;

public enum ProcessingEventType {

    /**
     * Задача на оценку полезности материала
     */
    RATE_RAW_MATERIAL,
    /**
     * Материал со слишком низкой оценкой - не подлежит пересказу и публикации
     * Терминальный статус
     */
    UNDERRATED,
    /**
     * Задача на пересказ
     */
    RETELLING,
    /**
     * Задача на публикацию пересказа
     */
    PUBLISH_RETELLING,
    /**
     * Опубликован, терминальный статус
     */
    PUBLISHED,
    /**
     * Ошибка публикации, терминальный статус
     */
    PUBLICATION_ERROR,
    /**
     * Шаг преобразования
     */
    MAP
}
