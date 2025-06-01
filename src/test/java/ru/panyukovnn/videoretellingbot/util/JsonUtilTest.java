package ru.panyukovnn.videoretellingbot.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonUtilTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private JsonUtil jsonUtil;

    @Test
    void when_fromJson_then_success() throws Exception {
        // Arrange
        String json = "{\"name\":\"test\"}";
        TestClass expected = new TestClass("test");
        when(objectMapper.readValue(eq(json), eq(TestClass.class))).thenReturn(expected);

        // Act
        TestClass result = jsonUtil.fromJson(json, TestClass.class);

        // Assert
        assertEquals(expected, result);
        verify(objectMapper).readValue(json, TestClass.class);
    }

    @Test
    void when_toJson_then_success() throws Exception {
        // Arrange
        TestClass obj = new TestClass("test");
        String expected = "{\"name\":\"test\"}";
        when(objectMapper.writeValueAsString(eq(obj))).thenReturn(expected);

        // Act
        String result = jsonUtil.toJson(obj);

        // Assert
        assertEquals(expected, result);
        verify(objectMapper).writeValueAsString(obj);
    }

    private static class TestClass {
        private String name;

        public TestClass() {
        }

        public TestClass(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestClass testClass = (TestClass) o;
            return name.equals(testClass.name);
        }
    }
} 