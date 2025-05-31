package ru.panyukovnn.videoretellingbot.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEventType;

@Getter
@RequiredArgsConstructor
public enum ConveyorType {

    /**
     * RETELLING -> PUBLISH_RETELLING -> PUBLISHED
     *                                   PUBLICATION_ERROR
     */
    JUST_RETELLING(ProcessingEventType.RETELLING),
    /**
     * RATE_RAW_MATERIAL -> RETELLING -> PUBLISH_RETELLING -> PUBLISHED
     *                      UNDERRATED -> x                   PUBLICATION_ERROR
     */
    RATING_AND_RETELLING(ProcessingEventType.RATE_RAW_MATERIAL),
    /**
     * MAP           -> REDUCE         -> PUBLISHING -> PUBLISHED
     * MAPPING_ERROR    REDUCING_ERROR                  PUBLICATION_ERROR
     */
    MAP_REDUCE(ProcessingEventType.MAP);

    private final ProcessingEventType startEventType;
}
