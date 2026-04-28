package io.github.spring.middleware.ai.rag.index;

import io.github.spring.middleware.ai.rag.source.DocumentSource;
import reactor.core.publisher.Mono;

public interface DocumentIndexer<O extends DocumentIndexerOptions> {

    Mono<Void> index(DocumentSource source, O options);

    boolean supports(DocumentIndexerType indexerType);

}
