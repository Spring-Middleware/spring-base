package io.github.spring.middleware.ai.rag.vector;

public interface VectorStoreRegistry {

    VectorStore findByType(VectorType vectorType);

}
