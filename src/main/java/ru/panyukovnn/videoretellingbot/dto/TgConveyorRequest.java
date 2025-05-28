package ru.panyukovnn.videoretellingbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TgConveyorRequest {

    @Schema(description = "Публичное имя канала, должно иметь точное соответствие")
    private String publicChatName;

    @Schema(description = "Часть имени приватного чата")
    private String privateChatNamePart;

    @Schema(description = "Имя топика, должно иметь точное соответствие")
    private String topicName;

    @Schema(description = "Максимальное количество сообщений")
    private Integer limit;

    @Schema(description = "Дата, от которой будут найдены сообщений")
    private LocalDateTime dateFrom;

    @Schema(description = "Дата, до которой будут найдены сообщения")
    private LocalDateTime dateTo;

    @NotEmpty
    @Schema(description = "Промт, который будет применяться к каждой группе извлеченных сообщений")
    private String mapPrompt;

    @Schema(description = "Промт, который будет агрегировать результаты mapPrompt")
    private String reducePrompt;

    @NotEmpty
    @Schema(description = "Идентификатор для поиска канала публикации")
    private String publishingChannelExternalId;

    @Schema(description = "Идентификатор чата публикации (игнорируется при указанном externalId)")
    private Long chatId;

    @Schema(description = "Идентификатор топика публикации (игнорируется при указанном externalId)")
    private Long topicId;

} 