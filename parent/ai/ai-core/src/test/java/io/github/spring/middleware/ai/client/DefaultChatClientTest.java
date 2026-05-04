package io.github.spring.middleware.ai.client;

import io.github.spring.middleware.ai.provider.AIProvider;
import io.github.spring.middleware.ai.provider.ProviderChatClient;
import io.github.spring.middleware.ai.registry.AIProviderRegistry;
import io.github.spring.middleware.ai.request.ChatRequest;
import io.github.spring.middleware.ai.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class DefaultChatClientTest {

    private DefaultChatClient chatClient;
    private AIProviderRegistry registry;
    private AIProvider provider;
    private ProviderChatClient providerChatClient;

    @BeforeEach
    void setUp() {
        registry = Mockito.mock(AIProviderRegistry.class);
        provider = Mockito.mock(AIProvider.class);
        providerChatClient = Mockito.mock(ProviderChatClient.class);
        chatClient = new DefaultChatClient(registry);
    }

    @Test
    void shouldGenerateResponseGivenValidRequest() {
        ChatRequest request = Mockito.mock(ChatRequest.class);
        String model = "model-1";
        when(request.getModel()).thenReturn(model);
        when(registry.resolve(model)).thenReturn(provider);
        when(provider.getChatClient()).thenReturn(providerChatClient);

        ChatResponse expectedResponse = Mockito.mock(ChatResponse.class);
        when(providerChatClient.generate(request)).thenReturn(Mono.just(expectedResponse));

        ChatResponse actualResponse = chatClient.generate(request).block();

        assertEquals(expectedResponse, actualResponse);
    }
}

