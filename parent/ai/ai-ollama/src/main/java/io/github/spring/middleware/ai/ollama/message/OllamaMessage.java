package io.github.spring.middleware.ai.ollama.message;

public record OllamaMessage(
        String role,
        String content
) {
}