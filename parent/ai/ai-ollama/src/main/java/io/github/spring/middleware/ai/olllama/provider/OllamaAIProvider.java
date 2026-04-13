package io.github.spring.middleware.ai.olllama.provider;

import io.github.spring.middleware.ai.olllama.config.OllamaAIProperties;
import io.github.spring.middleware.ai.provider.AbstractAIProvider;
import io.github.spring.middleware.ai.provider.ProviderChatClient;
import org.springframework.stereotype.Component;

@Component
public class OllamaAIProvider extends AbstractAIProvider {

    private final ProviderChatClient chatClient;

    public OllamaAIProvider(OllamaAIProperties properties,
                            ProviderChatClient chatClient) {
        super(properties);
        this.chatClient = chatClient;
    }

    @Override
    public ProviderChatClient getChatClient() {
        return chatClient;
    }
}
