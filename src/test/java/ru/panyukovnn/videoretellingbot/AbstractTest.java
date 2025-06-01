package ru.panyukovnn.videoretellingbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import ru.panyukovnn.videoretellingbot.client.OpenAiClient;
import ru.panyukovnn.videoretellingbot.config.TgBotApi;
import ru.panyukovnn.videoretellingbot.listener.TgBotListener;
import ru.panyukovnn.videoretellingbot.repository.*;
import ru.panyukovnn.videoretellingbot.serivce.autodatafinder.impl.HabrDataFinder;
import ru.panyukovnn.videoretellingbot.serivce.loader.impl.HabrLoader;
import ru.panyukovnn.videoretellingbot.serivce.loader.impl.YoutubeSubtitlesLoader;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
public abstract class AbstractTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected HabrLoader habrLoader;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected TgBotListener tgBotListener;
    @Autowired
    protected HabrDataFinder habrDataFinder;
    @Autowired
    protected ClientRepository clientRepository;
    @Autowired
    protected PromptRepository promptRepository;
    @Autowired
    protected ContentRepository contentRepository;
    @Autowired
    protected ProcessingEventRepository processingEventRepository;
    @Autowired
    protected PublishingChannelRepository publishingChannelRepository;

    @MockBean
    protected TgBotApi tgBotApi;
    @MockBean
    protected OpenAiClient openAiClient;
    @MockBean
    protected YoutubeSubtitlesLoader youtubeSubtitlesLoader;
}