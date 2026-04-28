package io.github.spring.middleware.ai.rag.context;

import io.github.spring.middleware.ai.rag.chunk.DocumentChunk;

import java.util.List;

public record RagContext(
        String content,
        List<DocumentChunk> chunks
) {
}
