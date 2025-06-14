package ru.panyukovnn.videoretellingbot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import ru.panyukovnn.videoretellingbot.client.OpenAiClient;
import ru.panyukovnn.videoretellingbot.config.TgBotApi;
import ru.panyukovnn.videoretellingbot.listener.TgBotListener;
import ru.panyukovnn.videoretellingbot.repository.ClientRepository;
import ru.panyukovnn.videoretellingbot.serivce.YoutubeSubtitlesLoader;

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
    protected OpenAiClient openAiClient;
    @MockBean
    protected YoutubeSubtitlesLoader youtubeSubtitlesLoader;
}