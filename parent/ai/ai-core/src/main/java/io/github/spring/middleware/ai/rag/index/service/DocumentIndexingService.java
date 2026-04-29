package io.github.spring.middleware.ai.rag.index.service;

import io.github.spring.middleware.ai.rag.index.DocumentClassifierParameters;
import io.github.spring.middleware.ai.rag.index.DocumentIndexerOptions;
import io.github.spring.middleware.ai.rag.source.DocumentSource;
import reactor.core.publisher.Mono;

public interface DocumentIndexingService {

    Mono<Void> indexSource(
            String sourceName,
            DocumentClassifierParameters<?> parameters
    );

    <I extends DocumentIndexerOptions> Mono<Void> indexDocumentSource(
            String sourceName,
            DocumentSource documentSource,
            DocumentClassifierParameters<?> parameters
    );

}
