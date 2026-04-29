package io.github.spring.middleware.ai.rag.index.config;

import io.github.spring.middleware.ai.rag.index.DocumentIndexerType;
import io.github.spring.middleware.ai.rag.vector.VectorType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
@ConfigurationProperties("middleware.ai.document.indexing")
public class DocumentIndexingProperties {

    private boolean indexOnStartup = false;
    private Map<String, DocumentIndexingSourceProperties> sources = new HashMap<>();

    @Data
    public static class DocumentIndexingSourceProperties {

        private VectorType vectorType = VectorType.MONGO;
        private String vectorNamespace;
        private String embeddingModel;
        private int topK = 5;
        // optional
        private DocumentIndexerType documentIndexerType = DocumentIndexerType.CHUNKER;

        public String getVectorNamespace(String sourceName) {
            return Optional.ofNullable(vectorNamespace)
                    .orElseGet(() -> STR."\{sourceName}_\{documentIndexerType.name().toLowerCase()}_\{embeddingModel.replaceAll("\\s+", "_").toLowerCase()}");
        }

        public String getVectorNamespace() {
            return Optional.ofNullable(vectorNamespace)
                    .orElseThrow(() -> new IllegalArgumentException("vectorNamespace must be provided if not using sourceName as namespace"));
        }

    }

}
