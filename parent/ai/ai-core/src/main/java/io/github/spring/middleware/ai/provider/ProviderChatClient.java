package io.github.spring.middleware.ai.provider;

import io.github.spring.middleware.ai.client.AIClient;import io.github.spring.middleware.ai.request.ChatRequest;
import io.github.spring.middleware.ai.response.ChatResponse;

public interface ProviderChatClient extends ProviderAIClient<ChatRequest, ChatResponse> {

    ChatResponse generate(ChatRequest request);

}
