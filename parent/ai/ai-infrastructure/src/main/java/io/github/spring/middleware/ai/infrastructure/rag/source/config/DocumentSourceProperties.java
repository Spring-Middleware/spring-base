package io.github.spring.middleware.ai.infrastructure.rag.source.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;import java.util.Map;

@Data
@ConfigurationProperties(prefix = "middleware.ai.document")
public class DocumentSourceProperties {

    private Map<String, DocumentSourceDefinition> sources = new HashMap<>();

}

