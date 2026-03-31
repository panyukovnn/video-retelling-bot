package ru.panyukovnn.videoretellingbot.listener;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.panyukovnn.videoretellingbot.AbstractTest;
import ru.panyukovnn.videoretellingbot.model.Client;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

import reactor.core.publisher.Flux;
import ru.panyukovnn.longpollingtgbotstarter.service.StreamingMessageUpdater;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TgBotListenerTest extends AbstractTest {

    @Test
    @Transactional
    void when_onUpdate_then_success() throws Exception {
        Update update = createUpdate();

        when(ytSubtitlesTool.loadSubtitles(any())).thenReturn("subtitles text");
        when(tgSender.sendStreaming(any())).thenReturn(mock(StreamingMessageUpdater.class));
        when(aiClient.startRetellingStream(any(), any(), any(), any()))
            .thenReturn(Flux.just("test retelling"));

        tgBotListener.onUpdate(update).get();

        List<Client> dbClients = clientRepository.findAll();
        assertThat(dbClients)
            .hasSize(1)
            .allSatisfy(client -> {
                assertEquals(123L, client.getTgUserId());
                assertEquals(123L, client.getTgLastChatId());
                assertEquals("username", client.getUsername());
                assertEquals("firstName", client.getFirstname());
                assertEquals("lastName", client.getLastname());
                assertEquals(1L, client.getRetellingsCount());
                assertNotNull(client.getCreateTime());
                assertNotNull(client.getLastUpdateTime());
                assertNotNull(client.getCreateUser());
                assertNotNull(client.getLastUpdateUser());
            });
    }

    private static Update createUpdate() {
        User user = User.builder()
            .id(123L)
            .firstName("firstName")
            .lastName("lastName")
            .userName("username")
            .isBot(false)
            .build();

        Chat chat = Chat.builder()
            .id(123L)
            .type("private")
            .build();

        Message message = new Message();
        message.setFrom(user);
        message.setChat(chat);
        message.setText("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        message.setDate((int) Instant.now().getEpochSecond());

        Update update = new Update();
        update.setMessage(message);

        return update;
    }
}