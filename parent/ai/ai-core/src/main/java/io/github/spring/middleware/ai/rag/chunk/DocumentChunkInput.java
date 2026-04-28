package io.github.spring.middleware.ai.rag.chunk;

import java.util.Map;

public record DocumentChunkInput(
        String content,
        Map<String, String> metadata
) {
}
