package io.github.spring.middleware.ai.infrastructure.rag.vector.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("middleware.ai.vector-store.qdrant")
public class QdrantProperties {

    private boolean enabled;
    private String url;
}