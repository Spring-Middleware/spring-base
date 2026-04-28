package io.github.spring.middleware.ai.rag.index.config;

import io.github.spring.middleware.ai.rag.vector.VectorType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("middleware.ai.document.indexing")
public class DocumentIndexingProperties {

    private boolean enabled = true;
    private boolean indexOnStartup = false;
    private VectorType vectorType;
    private String embeddingModel;
    private int topK = 5;

}
