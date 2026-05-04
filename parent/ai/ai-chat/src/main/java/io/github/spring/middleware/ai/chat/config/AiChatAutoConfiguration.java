package io.github.spring.middleware.ai.chat.config;

import io.github.spring.middleware.ai.client.EmbeddingClient;
import io.github.spring.middleware.ai.rag.context.DefaultRagContextBuilder;
import io.github.spring.middleware.ai.rag.context.RagContextBuilder;
import io.github.spring.middleware.ai.rag.vector.VectorStoreRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class AiChatAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RagContextBuilder.class)
    public RagContextBuilder ragContextBuilder(
            EmbeddingClient embeddingClient,
            VectorStoreRegistry vectorStoreRegistry
    ) {
        return new DefaultRagContextBuilder(embeddingClient, vectorStoreRegistry);
    }
}

