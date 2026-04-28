package io.github.spring.middleware.ai.rag.vector.config;

import io.github.spring.middleware.ai.rag.vector.DefaultVectorStoreRegistry;
import io.github.spring.middleware.ai.rag.vector.VectorStore;
import io.github.spring.middleware.ai.rag.vector.VectorStoreRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
public class VectorStoreRegistryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(VectorStoreRegistry.class)
    public VectorStoreRegistry vectorStoreRegistry(List<VectorStore> vectorStores) {
        return new DefaultVectorStoreRegistry(vectorStores);
    }
}
