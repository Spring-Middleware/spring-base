package io.github.spring.middleware.ai.rag.index.options;

import io.github.spring.middleware.ai.rag.chunk.ChunkerOptions;
import io.github.spring.middleware.ai.rag.chunk.DocumentChunker;
import io.github.spring.middleware.ai.rag.chunk.config.DefaultDocumentChunkerProperties;
import io.github.spring.middleware.ai.rag.chunk.config.MarkdownDocumentChunkerProperties;
import io.github.spring.middleware.ai.rag.chunk.deflt.ChunkOptions;
import io.github.spring.middleware.ai.rag.chunk.markdown.MarkdownChunkerOptions;
import io.github.spring.middleware.ai.rag.chunk.registry.DocumentChunkerRegistry;
import io.github.spring.middleware.ai.rag.source.DocumentSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultDocumentChunkerOptionsResolver implements DocumentChunkerOptionsResolver {

    private final DocumentChunkerRegistry documentChunkerRegistry;
    private final DefaultDocumentChunkerProperties defaultDocumentChunkerProperties;
    private final MarkdownDocumentChunkerProperties markdownDocumentChunkerProperties;

    @Override
    public <O extends ChunkerOptions> O resolve(DocumentSource documentSource, O options) {

        DocumentChunker<?> documentChunker = documentChunkerRegistry.findBestDocumentChunker(documentSource);
        Class<?> expectedType = documentChunker.optionsType();
        if (options != null && !expectedType.isInstance(options)) {
            return (O) createDefaultOptions(expectedType);
        } else if (options == null) {
            return (O) createDefaultOptions(expectedType);
        } else {
            return options;
        }
    }

    private <O extends ChunkerOptions> O createDefaultOptions(Class<?> expectedType) {
        if (ChunkOptions.class.equals(expectedType)) {
            return (O) new ChunkOptions(defaultDocumentChunkerProperties.getChunkSize(), defaultDocumentChunkerProperties.getOverlapSize());
        } else if (MarkdownChunkerOptions.class.equals(expectedType)) {
            return (O) new MarkdownChunkerOptions(
                    markdownDocumentChunkerProperties.getMaxChars(),
                    markdownDocumentChunkerProperties.getMinChars(),
                    markdownDocumentChunkerProperties.isSplitByHeadings(),
                    markdownDocumentChunkerProperties.getMaxHeadingLevel(),
                    markdownDocumentChunkerProperties.isIncludeHeadingInChunk(),
                    markdownDocumentChunkerProperties.isPreserveHeadingPath(),
                    markdownDocumentChunkerProperties.isSplitByParagraphs(),
                    markdownDocumentChunkerProperties.isSplitBySentences(),
                    markdownDocumentChunkerProperties.isDetectCodeBlocks(),
                    markdownDocumentChunkerProperties.isKeepCodeBlocksIntact(),
                    markdownDocumentChunkerProperties.isTrim(),
                    markdownDocumentChunkerProperties.isRemoveEmptyLines(),
                    markdownDocumentChunkerProperties.isIncludeLineNumbers()
            );
        } else {
            throw new IllegalStateException(STR."No default options available for chunker options type: \{expectedType.getName()}");
        }
    }

}
