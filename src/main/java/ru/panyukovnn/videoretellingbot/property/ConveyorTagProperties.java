package ru.panyukovnn.videoretellingbot.property;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import ru.panyukovnn.videoretellingbot.exception.ConveyorException;
import ru.panyukovnn.videoretellingbot.exception.InvalidProcessingEventException;
import ru.panyukovnn.videoretellingbot.model.ConveyorTag;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Setter
@Component
@ConfigurationProperties(prefix = "retelling")
public class ConveyorTagProperties {

    private Map<String, ConveyorTagConfig> conveyorTagConfigMap;

    @PostConstruct
    public void validateOnStartup() {
        List<ConveyorTag> notConfiguredTags = Arrays.stream(ConveyorTag.values())
            .filter(conveyorTag -> !conveyorTagConfigMap.containsKey(conveyorTag.getPropertyValue()))
            .toList();

        if (!notConfiguredTags.isEmpty()) {
            throw new ConveyorException("7d1e", "Ассоциативный массив настроек для тегов конвейеров не содержит часть предусмотренных значений: " + notConfiguredTags);
        }
    }

    public ConveyorTagConfig getWithGuarantee(ConveyorTag tag) {
        String tagPropertyValue = tag.getPropertyValue();

        ConveyorTagConfig conveyorTagConfig = conveyorTagConfigMap.get(tagPropertyValue);
        if (conveyorTagConfig == null) {
            throw new InvalidProcessingEventException("4e27", "Не удалось определить набор конфигураций по тегу: " + tag);
        }

        return conveyorTagConfig;
    }

    @Data
    public static class ConveyorTagConfig {
        private String rateMaterialPrompt;
        private String retellingPrompt;
        private Long publishingTopicId;
    }
}
