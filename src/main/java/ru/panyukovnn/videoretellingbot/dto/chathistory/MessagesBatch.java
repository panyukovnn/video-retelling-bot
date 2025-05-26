package ru.panyukovnn.videoretellingbot.dto.chathistory;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MessagesBatch {

    private Integer count;
    private List<MessageDto> messages;
}
