package ru.panyukovnn.videoretellingbot.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "retelling.prompt")
public class PromptProperties {

    private String youtubeRetelling;
}
