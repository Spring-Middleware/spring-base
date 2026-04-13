package io.github.spring.middleware.ai.olllama.request;

import io.github.spring.middleware.ai.olllama.message.OllamaMessage;

import java.util.List;

public record OllamaChatRequest (
        String model,
        List<OllamaMessage> messages,
        boolean stream
) {
}
