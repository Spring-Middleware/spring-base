package io.github.spring.middleware.ai.rag.chunk.registry;

import io.github.spring.middleware.ai.rag.chunk.DocumentChunker;
import io.github.spring.middleware.ai.rag.source.DocumentSource;

public interface DocumentChunkerRegistry {

    DocumentChunker findBestDocumentChunker(DocumentSource documentSource);

}
