package io.github.spring.middleware.ai.rag.chunk.markdown;

import io.github.spring.middleware.ai.rag.chunk.ChunkerOptions;

public record MarkdownChunkerOptions(
    int maxChars,
    int minChars,
    boolean splitByHeadings,
    int maxHeadingLevel,
    boolean includeHeadingInChunk,
    boolean preserveHeadingPath,
    boolean splitByParagraphs,
    boolean splitBySentences,
    boolean detectCodeBlocks,
    boolean keepCodeBlocksIntact,
    boolean trim,
    boolean removeEmptyLines,
    boolean includeLineNumbers
) implements ChunkerOptions {

    public MarkdownChunkerOptions() {
        this(2000, 200, true, 6, true, true, true, false, true, true, true, false, false);
    }
}
