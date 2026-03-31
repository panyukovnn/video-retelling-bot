package ru.panyukovnn.videoretellingbot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.panyukovnn.longpollingtgbotstarter.config.TgBotApi;
import ru.panyukovnn.longpollingtgbotstarter.service.TgSender;
import ru.panyukovnn.videoretellingbot.client.AiClient;
import ru.panyukovnn.videoretellingbot.listener.TgBotListener;
import ru.panyukovnn.videoretellingbot.repository.ClientRepository;
import ru.panyukovnn.videoretellingbot.tool.YtSubtitlesTool;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractTest {

    @Autowired
    protected TgBotListener tgBotListener;
    @Autowired
    protected ClientRepository clientRepository;

    @MockBean
    protected TelegramBotsLongPollingApplication telegramBotsLongPollingApplication;
    @MockBean
    protected TgBotApi tgBotApi;
    @MockBean
    protected TelegramClient telegramClient;
    @MockBean
    protected AiClient aiClient;
    @MockBean
    protected YtSubtitlesTool ytSubtitlesTool;
    @MockBean
    protected TgSender tgSender;
}