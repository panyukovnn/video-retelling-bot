package ru.panyukovnn.videoretellingbot.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JsonUtil {

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public <T> T fromJson(String rawValue, Class<T> clazz) {
        return objectMapper.readValue(rawValue, clazz);
    }

    @SneakyThrows
    public <T> String toJson(T object) {
        return objectMapper.writeValueAsString(object);
    }
}
