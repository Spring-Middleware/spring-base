package io.github.spring.middleware.ai.rag.chunk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "middleware.ai")
public class DocumentChunkerConfiguration {

    private Map<String, DocumentChunkerProperties> documentChunkers;


    public Map<String, DocumentChunkerProperties> getDocumentChunkers() {
        return documentChunkers;
    }

    public void setDocumentChunkers(Map<String, DocumentChunkerProperties> documentChunkers) {
        this.documentChunkers = documentChunkers;
    }
}
