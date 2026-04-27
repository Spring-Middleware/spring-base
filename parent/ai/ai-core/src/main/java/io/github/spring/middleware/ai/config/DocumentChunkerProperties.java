package io.github.spring.middleware.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "middleware.ai.document-chunker")
public class DocumentChunkerProperties {

    private Integer chunkSize = 800;
    private Integer overlapSize = 150;

}
