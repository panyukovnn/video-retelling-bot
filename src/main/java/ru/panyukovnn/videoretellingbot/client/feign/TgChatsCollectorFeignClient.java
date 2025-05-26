package ru.panyukovnn.videoretellingbot.client.feign;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.panyukovnn.videoretellingbot.dto.chathistory.ChatHistoryResponse;

import java.time.LocalDateTime;

@FeignClient(url = "${retelling.integration.tg-chats-collector.host}/tg-chats-collector/api/v1", name = "tg-chats-collector")
public interface TgChatsCollectorFeignClient {

    @GetMapping("/getChatHistory")
    ChatHistoryResponse getChatHistory(@RequestParam(required = false) String publicChatName,
                                       @RequestParam(required = false) String privateChatNamePart,
                                       @RequestParam(required = false) String topicName,
                                       @RequestParam(required = false) Integer limit,
                                       @Schema(description = "Дата до которой будут извлекаться сообщения, в UTC")
                                       @RequestParam(required = false) LocalDateTime dateFrom,
                                       @RequestParam(required = false) LocalDateTime dateTo);

}
