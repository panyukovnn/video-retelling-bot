package ru.panyukovnn.videoretellingbot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.panyukovnn.videoretellingbot.dto.TgConveyorRequest;
import ru.panyukovnn.videoretellingbot.dto.common.CommonRequest;
import ru.panyukovnn.videoretellingbot.dto.common.CommonResponse;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/conveyor/tg")
@RequiredArgsConstructor
public class TgConveyorController {

    @Async("tgMessageExtractorScheduler")
    @PostMapping("/processChat")
    public CompletableFuture<CommonResponse<Void>> processChat(@RequestBody @Valid CommonRequest<TgConveyorRequest> commonRequest) {


        return CompletableFuture.completedFuture(new CommonResponse<>());
    }
} 