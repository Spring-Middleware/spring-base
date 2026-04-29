package io.github.spring.middleware.ai.rag.chunk.registry;

import io.github.spring.middleware.ai.rag.chunk.ChunkerSuitability;
import io.github.spring.middleware.ai.rag.chunk.DocumentChunker;
import io.github.spring.middleware.ai.rag.source.DocumentSource;

import java.util.Comparator;
import java.util.List;

public class DefaultDocumentChunkerRegistry implements DocumentChunkerRegistry {

    private List<DocumentChunker> documentChunkers;

    public DefaultDocumentChunkerRegistry(List<DocumentChunker> documentChunkers) {
        this.documentChunkers = documentChunkers;
    }

    @Override
    public DocumentChunker findBestDocumentChunker(DocumentSource documentSource) {
        return documentChunkers.stream()
                .max(Comparator.comparingInt(chunker ->
                        chunker.suitability(documentSource)
                ))
                .filter(chunker -> chunker.suitability(documentSource) > ChunkerSuitability.UNSUPPORTED)
                .orElseThrow(() -> new IllegalStateException(
                        STR."No document chunker found for source: \{documentSource.title()}"
                ));
    }
}
