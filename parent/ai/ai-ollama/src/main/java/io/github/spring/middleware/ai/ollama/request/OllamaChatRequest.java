package io.github.spring.middleware.ai.ollama.request;

import io.github.spring.middleware.ai.ollama.message.OllamaMessage;

import java.util.List;

public record OllamaChatRequest (
        String model,
        List<OllamaMessage> messages,
        boolean stream
) {
}
