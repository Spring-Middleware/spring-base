package io.github.spring.middleware.ai.rag.chunk.registry;

import io.github.spring.middleware.ai.rag.chunk.DocumentChunker;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
public class DocumentChunkerRegistryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DocumentChunkerRegistry.class)
    public DocumentChunkerRegistry documentChunkerRegistry(List<DocumentChunker> chunkers) {
        return new DefaultDocumentChunkerRegistry(chunkers);
    }

}
