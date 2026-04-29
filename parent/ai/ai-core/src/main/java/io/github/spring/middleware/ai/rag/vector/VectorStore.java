package io.github.spring.middleware.ai.rag.vector;

import io.github.spring.middleware.ai.rag.chunk.DocumentChunk;

import java.util.List;
import java.util.Set;

public interface VectorStore {

    void add(VectorNamespace namespace, DocumentChunk chunk);

    List<DocumentChunk> search(VectorNamespace namespace, List<Float> embedding, int topK);

    boolean exists(
            VectorNamespace namespace,
            String documentId,
            String embeddingModel,
            String checksum
    );

    void deleteByDocumentIdAndEmbeddingModelExceptChecksums(
            VectorNamespace namespace,
            String documentId,
            String embeddingModel,
            Set<String> checksums
    );

    VectorType getType();
}