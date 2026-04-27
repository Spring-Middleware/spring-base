package io.github.spring.middleware.ai.provider;

import io.github.spring.middleware.ai.request.EmbeddingRequest;
import io.github.spring.middleware.ai.response.EmbeddingResponse;

public interface ProviderEmbeddingClient extends ProviderAIClient<EmbeddingRequest, EmbeddingResponse> {

    EmbeddingResponse generate(EmbeddingRequest request);

}
