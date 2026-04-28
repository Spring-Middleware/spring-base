package io.github.spring.middleware.ai.rag.index;

import io.github.spring.middleware.ai.rag.vector.VectorType;

public interface DocumentIndexerOptions {

    String getEmbeddingModel();

    DocumentIndexerType getIndexerType();

    VectorType getVectorType();

}
