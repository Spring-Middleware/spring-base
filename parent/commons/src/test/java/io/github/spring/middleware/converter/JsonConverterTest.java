package io.github.spring.middleware.converter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonConverterTest {

    private JsonConverter<TestData> testDataJsonConverter = new JsonConverter<>(TestData.class);

    @Test
    public void test() {
        TestData testData = TestData.builder()
                .text("Any text")
                .build();
        String json = testDataJsonConverter.toString(testData);
        assertEquals(json,"{\"text\":\"Any text\"}");

    }


}
