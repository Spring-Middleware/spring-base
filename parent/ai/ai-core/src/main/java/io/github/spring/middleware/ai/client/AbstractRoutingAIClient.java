package io.github.spring.middleware.ai.client;

import io.github.spring.middleware.ai.provider.AIProvider;
import io.github.spring.middleware.ai.provider.ProviderAIClient;
import io.github.spring.middleware.ai.registry.AIProviderRegistry;
import io.github.spring.middleware.ai.request.AIRequest;
import io.github.spring.middleware.ai.response.AIResponse;

import java.util.function.Function;

public abstract class AbstractRoutingAIClient<
        R extends AIRequest,
        S extends AIResponse,
        P extends ProviderAIClient<R, S>
        > implements AIClient<R, S> {

    private final AIProviderRegistry registry;
    private final Function<AIProvider, P> clientResolver;

    protected AbstractRoutingAIClient(
            AIProviderRegistry registry,
            Function<AIProvider, P> clientResolver
    ) {
        this.registry = registry;
        this.clientResolver = clientResolver;
    }

    @Override
    public S generate(R request) {
        AIProvider provider = registry.resolve(request.getModel());
        P client = clientResolver.apply(provider);
        return client.generate(request);
    }
}