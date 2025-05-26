package ru.panyukovnn.videoretellingbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import ru.panyukovnn.videoretellingbot.client.feign.TgChatsCollectorFeignClient;

@SpringBootApplication
@EnableFeignClients(clients = TgChatsCollectorFeignClient.class)
public class VideoRetellingBotApp {

    public static void main(String[] args) {
        SpringApplication.run(VideoRetellingBotApp.class, args);
    }
}
