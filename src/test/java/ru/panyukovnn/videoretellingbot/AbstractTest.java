package ru.panyukovnn.videoretellingbot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import ru.panyukovnn.longpollingtgbotstarter.config.TgBotApi;
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
    protected TgBotApi tgBotApi;
    @MockBean
    protected AiClient aiClient;
    @MockBean
    protected YtSubtitlesTool ytSubtitlesTool;
    @MockBean
    protected ru.panyukovnn.longpollingtgbotstarter.service.TgSender tgSender;
}