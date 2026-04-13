package io.github.spring.middleware.ai.olllama.message;

public record OllamaMessage(
        String role,
        String content
) {
}