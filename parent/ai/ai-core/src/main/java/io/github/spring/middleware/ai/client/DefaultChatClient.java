package io.github.spring.middleware.ai.client;

import io.github.spring.middleware.ai.provider.AIProvider;
import io.github.spring.middleware.ai.provider.ProviderChatClient;
import io.github.spring.middleware.ai.registry.AIProviderRegistry;
import io.github.spring.middleware.ai.request.ChatRequest;
import io.github.spring.middleware.ai.response.ChatResponse;
import org.springframework.stereotype.Component;

@Component
public class DefaultChatClient extends AbstractRoutingAIClient<
        ChatRequest,
        ChatResponse,
        ProviderChatClient
        > implements ChatClient {

    public DefaultChatClient(AIProviderRegistry registry) {
        super(registry, AIProvider::getChatClient);
    }
}
