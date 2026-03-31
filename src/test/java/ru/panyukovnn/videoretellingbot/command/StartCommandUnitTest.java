package ru.panyukovnn.videoretellingbot.command;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.panyukovnn.longpollingtgbotstarter.service.TgSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StartCommandUnitTest {

    private final TgSender tgSender = mock(TgSender.class);

    private final StartCommand startCommand = new StartCommand(tgSender);

    @Nested
    class Execute {

        @Test
        void when_execute_then_welcomeMessageSent() {
            Long chatId = 42L;

            startCommand.execute(chatId);

            verify(tgSender).send(eq(chatId), anyString());
        }

        @Test
        void when_execute_then_messageMentionsNewLinkResetsContext() {
            Long chatId = 42L;
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

            startCommand.execute(chatId);

            verify(tgSender).send(eq(chatId), messageCaptor.capture());
            assertThat(messageCaptor.getValue()).contains("сбрасывается");
        }

    }

}