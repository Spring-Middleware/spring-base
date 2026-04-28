package io.github.spring.middleware.ai.rag.vector;

import io.github.spring.middleware.ai.rag.chunk.DocumentChunk;

import java.util.List;
import java.util.Set;

public interface VectorStore {

    void add(DocumentChunk chunk);

    List<DocumentChunk> search(List<Float> embedding, int topK);

    VectorType getType();

    boolean exists(String documentId, String embeddingModel, String checksum);

    void deleteByDocumentIdAndEmbeddingModelExceptChecksums(
            String documentId,
            String embeddingModel,
            Set<String> checksums
    );

}