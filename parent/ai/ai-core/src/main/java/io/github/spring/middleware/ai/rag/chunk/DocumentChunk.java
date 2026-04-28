package io.github.spring.middleware.ai.rag.chunk;

import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DocumentChunk(
        @Id
        UUID id,
        String documentId,
        String title,
        String content,
        List<Float> embedding,
        String embeddingModel,
        Map<String, String> metadata,
        String checksum,
        Instant indexedAt
) {
}
