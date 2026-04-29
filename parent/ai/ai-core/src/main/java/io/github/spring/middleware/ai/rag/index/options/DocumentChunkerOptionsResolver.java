package io.github.spring.middleware.ai.rag.index.options;

import io.github.spring.middleware.ai.rag.chunk.ChunkerOptions;
import io.github.spring.middleware.ai.rag.source.DocumentSource;

public interface DocumentChunkerOptionsResolver {

    <O extends ChunkerOptions> O resolve(
            DocumentSource documentSource,
            O options
    );

}
