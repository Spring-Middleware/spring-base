package io.github.spring.middleware.ai.rag.index;

import io.github.spring.middleware.ai.rag.chunk.ChunkerOptions;
import io.github.spring.middleware.ai.rag.index.config.DocumentIndexingProperties.DocumentIndexingSourceProperties;
import io.github.spring.middleware.ai.rag.vector.VectorType;
import lombok.Data;

@Data
public class DocumentClassifierParameters<C extends ChunkerOptions> {

    private DocumentIndexerType documentIndexerType;
    private VectorType vectorType;
    private String vectorNamespace;
    private String embeddingModel;
    private String sourceName;
    private String chunkerName;
    private C chunkerOptions;

    public static <C extends ChunkerOptions> DocumentClassifierParameters<C> from(String sourceName, DocumentIndexingSourceProperties sourceProperties) {
        DocumentClassifierParameters<C> parameters = new DocumentClassifierParameters<>();
        parameters.setSourceName(sourceName);
        parameters.setChunkerName(sourceProperties.getChunker());
        parameters.setDocumentIndexerType(sourceProperties.getDocumentIndexerType());
        parameters.setVectorType(sourceProperties.getVectorType());
        parameters.setVectorNamespace(sourceProperties.getVectorNamespace());
        parameters.setEmbeddingModel(sourceProperties.getEmbeddingModel());
        return parameters;
    }
}
