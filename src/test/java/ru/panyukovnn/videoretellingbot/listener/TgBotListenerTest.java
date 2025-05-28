package ru.panyukovnn.videoretellingbot.listener;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.panyukovnn.videoretellingbot.AbstractTest;
import ru.panyukovnn.videoretellingbot.model.Client;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TgBotListenerTest extends AbstractTest {

    @Test
    @Transactional
    void when_onUpdate_then_success() throws ExecutionException, InterruptedException {
        Update update = createUpdate();

        when(openAiClient.promptingCall(any(), any(), any()))
            .thenReturn("test");

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
        User user = new User();
        user.setId(123L);
        user.setUserName("username");
        user.setFirstName("firstName");
        user.setLastName("lastName");

        Chat chat = new Chat();
        chat.setId(123L);

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