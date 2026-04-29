package io.github.spring.middleware.ai.rag.index.options;

import io.github.spring.middleware.ai.rag.index.DocumentIndexerOptions;
import io.github.spring.middleware.ai.rag.vector.VectorNamespace;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public abstract class AbstractDocumentIndexerOptions implements DocumentIndexerOptions {

    private final String embeddingModel;
    private final VectorNamespace vectorNamespace;

}
