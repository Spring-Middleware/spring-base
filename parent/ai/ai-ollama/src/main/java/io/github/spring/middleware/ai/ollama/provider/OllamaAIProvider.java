package io.github.spring.middleware.ai.ollama.provider;


import io.github.spring.middleware.ai.ollama.config.OllamaAIProperties;
import io.github.spring.middleware.ai.provider.AbstractAIProvider;
import io.github.spring.middleware.ai.provider.ProviderChatClient;
import io.github.spring.middleware.ai.provider.ProviderEmbeddingClient;
import io.github.spring.middleware.ai.provider.ProviderHealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class OllamaAIProvider extends AbstractAIProvider<OllamaAIProperties> {

    private final ProviderChatClient chatClient;
    private final ProviderEmbeddingClient embeddingClient;

    public OllamaAIProvider(OllamaAIProperties properties,
                            ProviderChatClient chatClient,
                            ProviderEmbeddingClient embeddingClient) {
        super(properties);
        this.chatClient = chatClient;
        this.embeddingClient = embeddingClient;
    }

    @Override
    public boolean supportsChat() {
        return true;
    }

    @Override
    public boolean supportsEmbeddings() {
        return true;
    }

    @Override
    public ProviderChatClient getChatClient() {
        return chatClient;
    }

    @Override
    public ProviderEmbeddingClient getEmbeddingClient() {
        return embeddingClient;
    }

    @Override
    public boolean isAvailable() {
        if (chatClient instanceof ProviderHealthIndicator healthIndicator) {
            return healthIndicator.isAvailable();
        }

        return true;
    }
}
