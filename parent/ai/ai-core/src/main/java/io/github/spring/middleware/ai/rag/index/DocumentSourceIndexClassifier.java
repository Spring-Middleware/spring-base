package io.github.spring.middleware.ai.rag.index;

import io.github.spring.middleware.ai.rag.chunk.ChunkOptions;
import io.github.spring.middleware.ai.rag.chunk.config.DocumentChunkerProperties;
import io.github.spring.middleware.ai.rag.index.chunker.ChunkerDocumentIndexerOptions;
import io.github.spring.middleware.ai.rag.index.config.DocumentIndexingProperties;
import io.github.spring.middleware.ai.rag.source.DocumentSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentSourceIndexClassifier {

    private final DocumentIndexingProperties indexingProperties;
    private final DocumentChunkerProperties chunkerProperties;

    public <O extends DocumentIndexerOptions> O classify(DocumentSource documentSource) {
        /// TODO: Implement classification logic based on documentSource attributes and properties
        ChunkOptions chunkOptions = new ChunkOptions(chunkerProperties.getChunkSize(), chunkerProperties.getOverlapSize());
        ChunkerDocumentIndexerOptions chunkerDocumentIndexerOptions = new ChunkerDocumentIndexerOptions(indexingProperties.getEmbeddingModel(), chunkOptions, indexingProperties.getVectorType());
        return (O) chunkerDocumentIndexerOptions;
    }
}




