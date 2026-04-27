package io.github.spring.middleware.ai.rag;

import io.github.spring.middleware.ai.config.DocumentChunkerProperties;
import reactor.core.publisher.Flux;

public interface DocumentChunker {


    Flux<DocumentChunkInput> chunk(
            DocumentSource source,
            DocumentChunkerProperties properties
    );

}