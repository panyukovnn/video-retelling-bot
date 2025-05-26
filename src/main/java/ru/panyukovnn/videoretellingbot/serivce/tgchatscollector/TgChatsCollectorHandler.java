package ru.panyukovnn.videoretellingbot.serivce.tgchatscollector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.panyukovnn.videoretellingbot.client.feign.TgChatsCollectorFeignClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class TgChatsCollectorHandler {

    private final TgChatsCollectorFeignClient tgChatsCollectorFeignClient;

}
