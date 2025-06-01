package ru.panyukovnn.videoretellingbot.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.panyukovnn.videoretellingbot.model.content.Lang;

import static org.junit.jupiter.api.Assertions.*;

class LanguageUtilsTest {

    @Test
    void when_detectLangByLettersCount_withEmptyText_then_returnUndefined() {
        // Act
        Lang result = LanguageUtils.detectLangByLettersCount("");

        // Assert
        assertEquals(Lang.UNDEFINED, result);
    }

    @Test
    void when_detectLangByLettersCount_withNullText_then_returnUndefined() {
        // Act
        Lang result = LanguageUtils.detectLangByLettersCount(null);

        // Assert
        assertEquals(Lang.UNDEFINED, result);
    }

    @ParameterizedTest
    @CsvSource({
        "Hello world, EN",
        "Hello мир, EN",
        "Привет world, RU",
        "Привет мир, RU",
        "123456789, UNDEFINED",
        "!@#$%^&*(), UNDEFINED"
    })
    void when_detectLangByLettersCount_then_returnCorrectLang(String text, Lang expectedLang) {
        // Act
        Lang result = LanguageUtils.detectLangByLettersCount(text);

        // Assert
        assertEquals(expectedLang, result);
    }

    @Test
    void when_detectLangByLettersCount_withMoreRussianLetters_then_returnRu() {
        // Arrange
        String text = "Привет world!";

        // Act
        Lang result = LanguageUtils.detectLangByLettersCount(text);

        // Assert
        assertEquals(Lang.RU, result);
    }

    @Test
    void when_detectLangByLettersCount_withMoreEnglishLetters_then_returnEn() {
        // Arrange
        String text = "Hello мир!";

        // Act
        Lang result = LanguageUtils.detectLangByLettersCount(text);

        // Assert
        assertEquals(Lang.EN, result);
    }

    @Test
    void when_detectLangByLettersCount_withMoreSpecialSymbols_then_returnUndefined() {
        // Arrange
        String text = "!@#$%^&*()_+";

        // Act
        Lang result = LanguageUtils.detectLangByLettersCount(text);

        // Assert
        assertEquals(Lang.UNDEFINED, result);
    }
}