package io.github.spring.middleware.ai.boot;

import io.github.spring.middleware.ai.client.EmbeddingClient;
import io.github.spring.middleware.ai.rag.context.DefaultRagContextBuilder;
import io.github.spring.middleware.ai.rag.context.RagContextBuilder;
import io.github.spring.middleware.ai.rag.vector.VectorStoreRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "io.github.spring.middleware")
public class DocumentationChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentationChatApplication.class, args);
    }

    @Bean
    @ConditionalOnMissingBean(RagContextBuilder.class)
    public RagContextBuilder ragContextBuilder(
            EmbeddingClient embeddingClient,
            VectorStoreRegistry vectorStoreRegistry
    ) {
        return new DefaultRagContextBuilder(embeddingClient, vectorStoreRegistry);
    }
}
