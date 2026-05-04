package io.github.spring.middleware.ai.provider;

import io.github.spring.middleware.ai.request.AIRequest;
import io.github.spring.middleware.ai.response.AIResponse;
import reactor.core.publisher.Mono;

public interface ProviderAIClient<R extends AIRequest, S extends AIResponse> {

    Mono<S> generate(R aiRequest);

}
