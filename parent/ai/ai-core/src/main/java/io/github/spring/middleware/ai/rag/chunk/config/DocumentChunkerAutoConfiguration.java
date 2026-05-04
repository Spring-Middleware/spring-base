package io.github.spring.middleware.ai.rag.chunk.config;

import io.github.spring.middleware.ai.rag.chunk.deflt.DefaultDocumentChunker;
import io.github.spring.middleware.ai.rag.chunk.DocumentChunker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@EnableConfigurationProperties({
        DocumentChunkerConfiguration.class
})
public class DocumentChunkerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DocumentChunker.class)
    public DocumentChunker documentChunker() {
        return new DefaultDocumentChunker();
    }

}
