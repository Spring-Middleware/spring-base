package io.github.spring.middleware.ai.provider;

import io.github.spring.middleware.ai.request.AIRequest;
import io.github.spring.middleware.ai.response.AIResponse;

public interface ProviderAIClient<R extends AIRequest, S extends AIResponse> {

    S generate(R aiRequest);

}
