package io.github.spring.middleware.ai.infrastructure.rag.vector.config;

import io.github.spring.middleware.ai.infrastructure.rag.vector.InMemoryVectorStore;
import io.github.spring.middleware.ai.infrastructure.rag.vector.MongoVectorStore;
import io.github.spring.middleware.ai.rag.vector.VectorStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

@AutoConfiguration
public class VectorStoreAutoConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "middleware.ai.vector-store.in-memory",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public VectorStore inMemoryVectorStore() {
        return new InMemoryVectorStore();
    }

    @Bean
    @ConditionalOnClass(MongoTemplate.class)
    @ConditionalOnProperty(
            prefix = "middleware.ai.vector-store.mongo",
            name = "enabled",
            havingValue = "true"
    )
    public VectorStore mongoVectorStore(MongoTemplate mongoTemplate) {
        return new MongoVectorStore(mongoTemplate);
    }
}
