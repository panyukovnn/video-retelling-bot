package ru.panyukovnn.videoretellingbot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.panyukovnn.videoretellingbot.dto.TgConveyorRequest;
import ru.panyukovnn.videoretellingbot.dto.common.CommonRequest;
import ru.panyukovnn.videoretellingbot.dto.common.CommonResponse;
import ru.panyukovnn.videoretellingbot.serivce.tgchatscollector.TgChatsCollectorHandler;

@Slf4j
@RestController
@RequestMapping("/api/v1/conveyor/tg")
@RequiredArgsConstructor
public class TgConveyorController {

    private final TgChatsCollectorHandler tgChatsCollectorHandler;

    @PostMapping("/processChat")
    public CommonResponse<Void> processChat(@RequestBody @Valid CommonRequest<TgConveyorRequest> commonRequest) {
        tgChatsCollectorHandler.handleChatMessages(commonRequest.getBody());

        return new CommonResponse<>();
    }
} 