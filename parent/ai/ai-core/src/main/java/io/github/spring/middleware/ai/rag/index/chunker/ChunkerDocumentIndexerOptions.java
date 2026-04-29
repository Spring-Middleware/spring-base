package io.github.spring.middleware.ai.rag.index.chunker;

import io.github.spring.middleware.ai.rag.chunk.ChunkerOptions;
import io.github.spring.middleware.ai.rag.index.options.AbstractDocumentIndexerOptions;
import io.github.spring.middleware.ai.rag.index.DocumentIndexerType;
import io.github.spring.middleware.ai.rag.vector.VectorNamespace;
import io.github.spring.middleware.ai.rag.vector.VectorType;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class ChunkerDocumentIndexerOptions<O extends ChunkerOptions> extends AbstractDocumentIndexerOptions {

    private final O chunkOptions;
    private final VectorType vectorType;

    public ChunkerDocumentIndexerOptions(String embeddingModel, O chunkOptions, VectorNamespace vectorNamespace, VectorType vectorType) {
        super(embeddingModel, vectorNamespace);
        this.chunkOptions = chunkOptions;
        this.vectorType = vectorType;
    }

    @Override
    public DocumentIndexerType getIndexerType() {
        return DocumentIndexerType.CHUNKER;
    }
}
