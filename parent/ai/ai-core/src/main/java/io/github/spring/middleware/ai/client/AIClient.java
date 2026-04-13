package io.github.spring.middleware.ai.client;

import io.github.spring.middleware.ai.request.AIRequest;
import io.github.spring.middleware.ai.response.AIResponse;

public interface AIClient<R extends AIRequest, S extends AIResponse> {

    S generate(R aiRequest);

}
