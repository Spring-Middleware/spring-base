package io.github.spring.middleware.ai.rag.chunk;

public record ResolvedDocumentChunker<O extends ChunkerOptions>(
        DocumentChunker<O> chunker,
        O options
) {
}
