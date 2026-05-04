package io.github.spring.middleware.ai.client;

import io.github.spring.middleware.ai.request.AIRequest;
import io.github.spring.middleware.ai.response.AIResponse;
import reactor.core.publisher.Mono;

public interface AIClient<R extends AIRequest, S extends AIResponse> {

    Mono<S> generate(R aiRequest);

}
