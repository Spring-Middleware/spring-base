package io.github.spring.middleware.ai.rag.context;

import io.github.spring.middleware.ai.rag.planner.MetadataFilter;
import io.github.spring.middleware.ai.rag.vector.VectorNamespace;
import io.github.spring.middleware.ai.rag.vector.VectorType;

import java.util.List;

public record RagContextRequest(
        String embeddingModel,
        VectorType vectorType,
        VectorNamespace vectorNamespace,
        String query,
        int topK,
        List<MetadataFilter> metadataFilters
) {
    public RagContextRequest(String embeddingModel, VectorType vectorType, VectorNamespace vectorNamespace, String query, int topK) {
        this(embeddingModel, vectorType, vectorNamespace, query, topK, List.of());
    }
}
