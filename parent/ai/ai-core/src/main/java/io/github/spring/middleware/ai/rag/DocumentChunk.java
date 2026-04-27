package io.github.spring.middleware.ai.rag;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DocumentChunk(
        UUID id,
        String documentId,
        String title,
        String content,
        List<Float> embedding,
        Map<String, String> metadata
) {
}
