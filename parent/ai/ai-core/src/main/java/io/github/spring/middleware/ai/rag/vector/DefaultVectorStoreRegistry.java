package io.github.spring.middleware.ai.rag.vector;

import java.util.List;

public class DefaultVectorStoreRegistry implements VectorStoreRegistry {

    private final List<VectorStore> vectorStores;

    public DefaultVectorStoreRegistry(List<VectorStore> vectorStores) {
        this.vectorStores = vectorStores;
    }

    @Override
    public VectorStore findByType(VectorType vectorType) {
        return vectorStores.stream()
                .filter(store -> store.getType() == vectorType)
                .findFirst().orElseThrow(() -> new IllegalArgumentException(STR."No VectorStore found for type: \{vectorType}"));
    }

}
