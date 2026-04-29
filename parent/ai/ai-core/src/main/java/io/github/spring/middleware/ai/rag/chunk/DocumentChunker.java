package io.github.spring.middleware.ai.rag.chunk;

import io.github.spring.middleware.ai.rag.source.DocumentSource;
import reactor.core.publisher.Flux;

public interface DocumentChunker<O extends ChunkerOptions> {

    Flux<DocumentChunkInput> chunk(
            DocumentSource source,
            O chunkOptions
    );

    int suitability(DocumentSource source);

    Class<O> optionsType();

}