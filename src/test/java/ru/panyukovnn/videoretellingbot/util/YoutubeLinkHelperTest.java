package ru.panyukovnn.videoretellingbot.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.panyukovnn.videoretellingbot.exception.RetellingException;

import static org.junit.jupiter.api.Assertions.*;

class YoutubeLinkHelperTest {

    @ParameterizedTest
    @CsvSource({
        "https://youtube.com/watch?v=test, https://youtube.com/watch?v=test",
        "https://youtube.com/watch?v=test&t=123, https://youtube.com/watch?v=test",
        "https://youtube.com/watch?v=test&feature=share, https://youtube.com/watch?v=test",
        "https://youtube.com/watch?v=test&t=123&feature=share, https://youtube.com/watch?v=test",
        "https://youtu.be/test, https://youtu.be/test",
        "https://youtu.be/test?t=123, https://youtu.be/test"
    })
    void when_removeRedundantQueryParamsFromYoutubeLint_then_success(String input, String expected) {
        // Act
        String result = YoutubeLinkHelper.removeRedundantQueryParamsFromYoutubeLint(input);

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void when_removeRedundantQueryParamsFromYoutubeLint_withInvalidUrl_then_throwException() {
        // Arrange
        String invalidUrl = "invalid url";

        // Act & Assert
        RetellingException exception = assertThrows(
            RetellingException.class,
            () -> YoutubeLinkHelper.removeRedundantQueryParamsFromYoutubeLint(invalidUrl)
        );

        assertEquals("4bc5", exception.getId());
        assertEquals("Невалидная ссылка youtube", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://youtube.com/watch?v=12345678901",
        "https://youtu.be/12345678901",
        "https://www.youtube.com/watch?v=12345678901",
        "https://www.youtube.com/shorts/12345678901",
        "https://youtube.com/shorts/12345678901",
        "https://www.youtube.com/live/12345678901",
        "https://youtube.com/live/12345678901"
    })
    void when_isValidYoutubeUrl_withValidUrls_then_returnTrue(String url) {
        // Act
        boolean result = YoutubeLinkHelper.isValidYoutubeUrl(url);

        // Assert
        assertTrue(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://youtube.com/watch",
        "https://youtube.com/watch?v=",
        "https://youtube.com/watch?v=invalid",
        "https://youtube.com/shorts",
        "https://youtube.com/live",
        "https://notyoutube.com/watch?v=test",
        "https://fakeyoutube.com/watch?v=test",
        "https://youtube.com",
        "https://youtube.com/",
        "https://youtube.com/invalid",
        "invalid url",
        "http://youtube.com/watch?v=test",
        "ftp://youtube.com/watch?v=test"
    })
    void when_isValidYoutubeUrl_withInvalidUrls_then_returnFalse(String url) {
        // Act
        boolean result = YoutubeLinkHelper.isValidYoutubeUrl(url);

        // Assert
        assertFalse(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://youtube.com/watch?v=12345678901",
        "https://youtu.be/12345678901",
        "https://www.youtube.com/watch?v=12345678901",
        "https://m.youtube.com/watch?v=12345678901",
        "https://music.youtube.com/watch?v=12345678901"
    })
    void when_isValidYouTubeHost_withValidHosts_then_returnTrue(String link) {
        // Act
        boolean result = YoutubeLinkHelper.isValidYoutubeUrl(link);

        // Assert
        assertTrue(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        ".youtube.com",
        "fakeyoutube.com",
        "youtube.com.fake",
        "youtube.com.",
        ".youtube.com.",
        "youtube.com.fake.com"
    })
    void when_isValidYouTubeHost_withInvalidHosts_then_returnFalse(String host) {
        // Act
        boolean result = YoutubeLinkHelper.isValidYoutubeUrl("https://" + host + "/watch?v=test");

        // Assert
        assertFalse(result);
    }
}