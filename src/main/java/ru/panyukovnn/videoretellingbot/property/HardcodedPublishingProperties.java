package ru.panyukovnn.videoretellingbot.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "retelling.hardcoded-publishing-channels")
public class HardcodedPublishingProperties {

    private Long chatId;
    private Long rateTgTopicId;
    /**
     * TODO использовать
     */
    private Long debugTopicId;

    private Long javaHabrTopicId;
    private Long tgMessageBatchTopicId;

}
