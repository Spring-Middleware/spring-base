package io.github.spring.middleware.ai.rag.chunk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
public class DefaultDocumentChunkerProperties {

    private Integer chunkSize = 800;
    private Integer overlapSize = 150;

}
