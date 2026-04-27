package io.github.spring.middleware.ai.client;

import io.github.spring.middleware.ai.provider.AIProvider;
import io.github.spring.middleware.ai.provider.ProviderEmbeddingClient;
import io.github.spring.middleware.ai.registry.AIProviderRegistry;
import io.github.spring.middleware.ai.request.EmbeddingRequest;
import io.github.spring.middleware.ai.response.EmbeddingResponse;
import org.springframework.stereotype.Component;

@Component
public class DefaultEmbeddingClient extends AbstractRoutingAIClient<
        EmbeddingRequest,
        EmbeddingResponse,
        ProviderEmbeddingClient
        > implements EmbeddingClient {

    public DefaultEmbeddingClient(AIProviderRegistry registry) {
        super(registry, AIProvider::getEmbeddingClient);
    }
}
