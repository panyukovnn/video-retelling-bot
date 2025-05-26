package ru.panyukovnn.videoretellingbot.dto.chathistory;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageDto {

    private Long id;
    private Long senderId;
    private String replyToText;
    private String text;
}
