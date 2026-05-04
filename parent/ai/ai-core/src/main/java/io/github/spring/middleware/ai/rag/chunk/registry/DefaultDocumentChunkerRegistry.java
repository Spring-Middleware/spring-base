package io.github.spring.middleware.ai.rag.chunk.registry;

import io.github.spring.middleware.ai.rag.chunk.ChunkerSuitability;
import io.github.spring.middleware.ai.rag.chunk.DocumentChunker;
import io.github.spring.middleware.ai.rag.source.DocumentSource;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DefaultDocumentChunkerRegistry implements DocumentChunkerRegistry {

    private final Map<String, DocumentChunker<?>> documentChunkers;

    public DefaultDocumentChunkerRegistry(Map<String, DocumentChunker<?>> documentChunkers) {
        this.documentChunkers = documentChunkers;
    }

    @Override
    public DocumentChunker findBestDocumentChunker(DocumentSource documentSource) {
        return documentChunkers.values().stream()
                .max(Comparator.comparingInt(chunker ->
                        chunker.suitability(documentSource)
                ))
                .filter(chunker -> chunker.suitability(documentSource) > ChunkerSuitability.UNSUPPORTED)
                .orElseThrow(() -> new IllegalStateException(
                        STR."No document chunker found for source: \{documentSource.title()}"
                ));
    }

    @Override
    public DocumentChunker<?> findByName(String chunkerName) {
        DocumentChunker<?> chunker = documentChunkers.get(chunkerName);

        if (chunker == null) {
            throw new IllegalStateException(STR."No document chunker found with name: \{chunkerName}");
        }

        return chunker;
    }

}
