package io.github.spring.middleware.ai.rag.chunk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
public class MarkdownDocumentChunkerProperties {

    private int maxChars = 2000;
    private int minChars = 200;

    // estructura
    private boolean splitByHeadings = true;
    private int maxHeadingLevel = 6;

    // comportamiento
    private boolean includeHeadingInChunk = true;
    private boolean preserveHeadingPath = true;

    // splitting interno
    private boolean splitByParagraphs = true;
    private boolean splitBySentences = false;

    // features markdown
    private boolean detectCodeBlocks = true;
    private boolean keepCodeBlocksIntact = true;

    // limpieza
    private boolean trim = true;
    private boolean removeEmptyLines = false;

    // metadata extra
    private boolean includeLineNumbers = false;

}
