package io.github.spring.middleware.ai.rag.index.chunker;

import io.github.spring.middleware.ai.rag.chunk.ChunkOptions;
import io.github.spring.middleware.ai.rag.index.AbstractDocumentIndexerOptions;
import io.github.spring.middleware.ai.rag.index.DocumentIndexerType;
import io.github.spring.middleware.ai.rag.vector.VectorType;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class ChunkerDocumentIndexerOptions extends AbstractDocumentIndexerOptions {

    private final ChunkOptions chunkOptions;
    private final VectorType vectorType;

    public ChunkerDocumentIndexerOptions(String embeddingModel, ChunkOptions chunkOptions, VectorType vectorType) {
        super(embeddingModel);
        this.chunkOptions = chunkOptions;
        this.vectorType = vectorType;
    }

    @Override
    public DocumentIndexerType getIndexerType() {
        return DocumentIndexerType.CHUNKER;
    }
}
