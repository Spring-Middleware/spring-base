package io.github.spring.middleware.ai.rag.index.options;

import io.github.spring.middleware.ai.rag.chunk.ChunkerOptions;
import io.github.spring.middleware.ai.rag.chunk.DocumentChunker;
import io.github.spring.middleware.ai.rag.chunk.config.DefaultDocumentChunkerProperties;
import io.github.spring.middleware.ai.rag.chunk.config.DocumentChunkerConfiguration;
import io.github.spring.middleware.ai.rag.chunk.config.DocumentChunkerProperties;
import io.github.spring.middleware.ai.rag.chunk.config.JsonDocumentChunkerProperties;
import io.github.spring.middleware.ai.rag.chunk.config.MarkdownDocumentChunkerProperties;
import io.github.spring.middleware.ai.rag.chunk.deflt.ChunkOptions;
import io.github.spring.middleware.ai.rag.chunk.json.JsonChunkerOptions;
import io.github.spring.middleware.ai.rag.chunk.markdown.MarkdownChunkerOptions;
import io.github.spring.middleware.ai.rag.chunk.registry.DocumentChunkerRegistry;
import io.github.spring.middleware.ai.rag.source.DocumentSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DefaultDocumentChunkerOptionsResolver implements DocumentChunkerOptionsResolver {

    private final DocumentChunkerRegistry documentChunkerRegistry;
    private final DocumentChunkerConfiguration documentChunkerConfiguration;

    @Override
    public <O extends ChunkerOptions> O resolve(String chunkerName, String sourceName, DocumentSource documentSource, O options) {

        DocumentChunker<?> documentChunker = documentChunkerRegistry.findByName(chunkerName);
        Class<?> expectedType = documentChunker.optionsType();
        if (options != null && !expectedType.isInstance(options)) {
            return (O) createDefaultOptions(chunkerName, sourceName, expectedType);
        } else if (options == null) {
            return (O) createDefaultOptions(chunkerName, sourceName, expectedType);
        } else {
            return options;
        }
    }

    private <O extends ChunkerOptions> O createDefaultOptions(String chunkerName, String sourceName, Class<?> expectedType) {
        DocumentChunkerProperties props = null;
        if (documentChunkerConfiguration.getDocumentChunkers() != null) {
            props = documentChunkerConfiguration.getDocumentChunkers().get(chunkerName);
        }
        if (props == null) {
            props = new DocumentChunkerProperties();
        }

        if (ChunkOptions.class.equals(expectedType)) {
            DefaultDocumentChunkerProperties defaultProps = props.getDefaultConfig();
            return (O) new ChunkOptions(defaultProps.getChunkSize(), defaultProps.getOverlapSize());
        } else if (MarkdownChunkerOptions.class.equals(expectedType)) {
            MarkdownDocumentChunkerProperties mdProps = props.getMarkdown();
            return (O) new MarkdownChunkerOptions(
                    mdProps.getMaxChars(),
                    mdProps.getMinChars(),
                    mdProps.isSplitByHeadings(),
                    mdProps.getMaxHeadingLevel(),
                    mdProps.isIncludeHeadingInChunk(),
                    mdProps.isPreserveHeadingPath(),
                    mdProps.isSplitByParagraphs(),
                    mdProps.isSplitBySentences(),
                    mdProps.isDetectCodeBlocks(),
                    mdProps.isKeepCodeBlocksIntact(),
                    mdProps.isTrim(),
                    mdProps.isRemoveEmptyLines(),
                    mdProps.isIncludeLineNumbers()
            );
        } else if (JsonChunkerOptions.class.equals(expectedType)) {
            JsonDocumentChunkerProperties jsonProps = props.getJson();
            return (O) Optional.ofNullable(jsonProps.getDefinitions().get(sourceName)).map(definitionProperties -> {
                return new JsonChunkerOptions(definitionProperties.getRulesPaths());
            }).orElseThrow(() -> new IllegalStateException(STR."No default JSON chunker options found for sourceName: \{sourceName}"));
        } else {
            throw new IllegalStateException(STR."No default options available for chunker options type: \{expectedType.getName()}");
        }
    }

}
