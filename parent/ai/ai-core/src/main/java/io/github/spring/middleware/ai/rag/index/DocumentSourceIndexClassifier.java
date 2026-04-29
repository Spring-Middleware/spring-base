package io.github.spring.middleware.ai.rag.index;

import io.github.spring.middleware.ai.rag.chunk.ChunkerOptions;
import io.github.spring.middleware.ai.rag.index.chunker.ChunkerDocumentIndexerOptions;
import io.github.spring.middleware.ai.rag.index.config.DocumentIndexingProperties;
import io.github.spring.middleware.ai.rag.index.options.DocumentChunkerOptionsResolver;
import io.github.spring.middleware.ai.rag.source.DocumentSource;
import io.github.spring.middleware.ai.rag.vector.VectorNamespace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DocumentSourceIndexClassifier {

    private final DocumentIndexingProperties indexingProperties;
    private final DocumentChunkerOptionsResolver documentChunkerOptionsResolver;

    @SuppressWarnings("unchecked")
    public <I extends DocumentIndexerOptions> I classify(String sourceName,
                                                         DocumentSource documentSource,
                                                         DocumentClassifierParameters classifierParameters
    ) {
        DocumentIndexingProperties.DocumentIndexingSourceProperties sourceProperties = indexingProperties.getSources().get(sourceName);
        if (sourceProperties == null) {
            throw new IllegalArgumentException(STR."No indexing properties found for sourceName: \{sourceName}");
        }
        return switch (classifierParameters.getDocumentIndexerType()) {
            case CHUNKER -> (I) new ChunkerDocumentIndexerOptions<>(
                    Optional.ofNullable(classifierParameters.getEmbeddingModel())
                            .orElse(sourceProperties.getEmbeddingModel()),
                    createChunkerOptions(documentSource, classifierParameters),
                    new VectorNamespace(
                            Optional.ofNullable(classifierParameters.getVectorNamespace())
                                    .orElse(sourceProperties.getVectorNamespace(sourceName))
                    ),
                    Optional.ofNullable(classifierParameters.getVectorType())
                            .orElse(sourceProperties.getVectorType()));

            default -> throw new IllegalArgumentException(
                    STR."Unsupported Document Indexer Type: \{classifierParameters.getDocumentIndexerType()}"
            );
        };
    }

    private ChunkerOptions createChunkerOptions(
            DocumentSource documentSource,
            DocumentClassifierParameters classifierParameters
    ) {
        return documentChunkerOptionsResolver.resolve(
                documentSource,
                classifierParameters.getChunkerOptions()
        );
    }
}




