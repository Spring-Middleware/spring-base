package io.github.spring.middleware.ai.provider;

import io.github.spring.middleware.ai.request.EmbeddingRequest;
import io.github.spring.middleware.ai.response.EmbeddingResponse;
import reactor.core.publisher.Mono;

public interface ProviderEmbeddingClient extends ProviderAIClient<EmbeddingRequest, EmbeddingResponse> {

    Mono<EmbeddingResponse> generate(EmbeddingRequest request);

}
