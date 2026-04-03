package ru.panyukovnn.videoretellingbot.command;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.panyukovnn.longpollingtgbotstarter.service.TgSender;
import ru.panyukovnn.videoretellingbot.property.FeedbackProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StartCommandUnitTest {

    private final TgSender tgSender = mock(TgSender.class);
    private final FeedbackProperties feedbackProperties = new FeedbackProperties();

    private final StartCommand startCommand = new StartCommand(tgSender, feedbackProperties);

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

        @Test
        void when_feedbackUrlConfigured_then_messageContainsFeedbackLink() {
            Long chatId = 42L;
            feedbackProperties.setFormUrl("https://forms.example.com/feedback");
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

            startCommand.execute(chatId);

            verify(tgSender).send(eq(chatId), messageCaptor.capture());
            assertThat(messageCaptor.getValue()).contains("https://forms.example.com/feedback");
        }

        @Test
        void when_feedbackUrlNotConfigured_then_messageDoesNotContainFeedbackSection() {
            Long chatId = 42L;
            feedbackProperties.setFormUrl(null);
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

            startCommand.execute(chatId);

            verify(tgSender).send(eq(chatId), messageCaptor.capture());
            assertThat(messageCaptor.getValue()).doesNotContain("отзыв");
        }

    }

}