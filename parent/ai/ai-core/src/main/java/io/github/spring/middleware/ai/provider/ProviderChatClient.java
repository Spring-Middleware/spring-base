package io.github.spring.middleware.ai.provider;

import io.github.spring.middleware.ai.request.ChatRequest;
import io.github.spring.middleware.ai.response.ChatResponse;

public interface ProviderChatClient {

    ChatResponse generate(ChatRequest request);

}
