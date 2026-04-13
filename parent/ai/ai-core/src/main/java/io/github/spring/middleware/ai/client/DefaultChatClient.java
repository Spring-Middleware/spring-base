package io.github.spring.middleware.ai.client;

import io.github.spring.middleware.ai.provider.AIProvider;
import io.github.spring.middleware.ai.provider.ProviderChatClient;
import io.github.spring.middleware.ai.registry.AIProviderRegistry;
import io.github.spring.middleware.ai.request.ChatRequest;
import io.github.spring.middleware.ai.response.ChatResponse;
import org.springframework.stereotype.Component;

@Component
public class DefaultChatClient implements ChatClient {

    private final AIProviderRegistry registry;

    public DefaultChatClient(AIProviderRegistry registry) {
        this.registry = registry;
    }

    @Override
    public ChatResponse generate(ChatRequest request) {
        AIProvider provider = registry.resolve(request.getModel());
        ProviderChatClient client = provider.getChatClient();
        return client.generate(request);
    }
}
