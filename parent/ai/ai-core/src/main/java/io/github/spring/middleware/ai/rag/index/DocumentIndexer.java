package io.github.spring.middleware.ai.rag.index;

import io.github.spring.middleware.ai.rag.chunk.ChunkerOptions;
import io.github.spring.middleware.ai.rag.source.DocumentSource;
import reactor.core.publisher.Mono;

public interface DocumentIndexer<I extends DocumentIndexerOptions> {

    <O extends ChunkerOptions> Mono<Void> index(String sourceName, DocumentSource source, I options);

    boolean supports(DocumentIndexerType indexerType);

}
