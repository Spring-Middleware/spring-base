package io.github.spring.middleware.ai.rag.context;

import io.github.spring.middleware.ai.rag.vector.VectorType;

public record RagContextRequest(
        String embeddingModel,
        VectorType vectorType,
        String query,
        int topK
) {
}
