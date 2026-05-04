package io.github.spring.middleware.ai.rag.chunk.config;

import lombok.Data;

@Data
public class DocumentChunkerProperties {
    
    private DocumentChunkerType type = DocumentChunkerType.DEFAULT;
    
    private DefaultDocumentChunkerProperties defaultConfig = new DefaultDocumentChunkerProperties();
    private MarkdownDocumentChunkerProperties markdown = new MarkdownDocumentChunkerProperties();
    private JsonDocumentChunkerProperties json = new JsonDocumentChunkerProperties();

    public enum DocumentChunkerType {
        DEFAULT, MARKDOWN, JSON
    }
}
