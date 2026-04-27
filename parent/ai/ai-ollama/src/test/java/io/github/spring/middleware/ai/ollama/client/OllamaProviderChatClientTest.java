package io.github.spring.middleware.ai.ollama.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.spring.middleware.ai.message.AIRole;
import io.github.spring.middleware.ai.message.DefaultAIMessage;
import io.github.spring.middleware.ai.ollama.config.OllamaAIProperties;
import io.github.spring.middleware.ai.request.DefaultChatRequest;
import io.github.spring.middleware.ai.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


class OllamaProviderChatClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(options().dynamicPort())
            .build();

    private OllamaProviderChatClient client;

    @BeforeEach
    void setUp() {
        OllamaAIProperties properties = new OllamaAIProperties();
        properties.setBaseUrl(wireMock.baseUrl());

        client = new OllamaProviderChatClient(properties, WebClient.builder());
    }

    @Test
    void shouldGenerateChatResponse() {
        wireMock.stubFor(post(urlEqualTo("/api/chat"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("llama3.1:8b")))
                .withRequestBody(matchingJsonPath("$.stream", equalTo("false")))
                .withRequestBody(matchingJsonPath("$.messages[0].role", equalTo("system")))
                .withRequestBody(matchingJsonPath("$.messages[0].content", equalTo("You are an expert in Spring Middleware")))
                .withRequestBody(matchingJsonPath("$.messages[1].role", equalTo("user")))
                .withRequestBody(matchingJsonPath("$.messages[1].content", equalTo("What is the registry?")))
                .willReturn(okJson("""
                        {
                          "model": "llama3.1:8b",
                          "created_at": "2026-04-09T10:00:00Z",
                          "message": {
                            "role": "assistant",
                            "content": "The registry is the control plane of the platform."
                          },
                          "done": true,
                          "total_duration": 123456789,
                          "load_duration": 123456,
                          "prompt_eval_count": 20,
                          "prompt_eval_duration": 1000000,
                          "eval_count": 12,
                          "eval_duration": 2000000
                        }
                        """)));

        DefaultChatRequest request = new DefaultChatRequest("llama3.1:8b");
        request.addMessage(new DefaultAIMessage(AIRole.SYSTEM, "You are an expert in Spring Middleware"));
        request.addMessage(new DefaultAIMessage(AIRole.USER, "What is the registry?"));

        ChatResponse response = client.generate(request);

        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertEquals(AIRole.ASSISTANT, response.getMessage().role());
        assertEquals("The registry is the control plane of the platform.", response.getMessage().content());

        wireMock.verify(postRequestedFor(urlEqualTo("/api/chat")));
    }

    @Test
    void shouldThrowExceptionWhenOllamaReturnsNullMessage() {
        wireMock.stubFor(post(urlEqualTo("/api/chat"))
                .willReturn(okJson("""
                        {
                          "model": "llama3.1:8b",
                          "created_at": "2026-04-09T10:00:00Z",
                          "message": null,
                          "done": true
                        }
                        """)));

        DefaultChatRequest request = new DefaultChatRequest("llama3.1:8b");
        request.addMessage(new DefaultAIMessage(AIRole.USER, "Hello"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> client.generate(request));

        assertNotNull(exception);
        wireMock.verify(postRequestedFor(urlEqualTo("/api/chat")));
    }
}
