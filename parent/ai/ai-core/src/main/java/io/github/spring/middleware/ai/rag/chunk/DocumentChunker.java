package io.github.spring.middleware.ai.rag.chunk;

import io.github.spring.middleware.ai.rag.source.DocumentSource;
import reactor.core.publisher.Flux;

public interface DocumentChunker {


    Flux<DocumentChunkInput> chunk(
            DocumentSource source,
            ChunkOptions chunkOptions
    );

}