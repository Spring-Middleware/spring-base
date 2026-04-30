package io.github.spring.middleware.ai.rag.chunk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "middleware.ai.document-chunker.json")
public class JsonDocumentChunkerProperties {

    private Map<String, JsonDocumentChunkerDefinitionProperties> definitions = new HashMap<>();

    @Data
    public static class JsonDocumentChunkerDefinitionProperties {
        private List<String> rulesPaths = new ArrayList<>();
    }
}
