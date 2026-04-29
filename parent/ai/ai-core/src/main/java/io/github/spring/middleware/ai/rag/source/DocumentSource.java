package io.github.spring.middleware.ai.rag.source;

import java.io.InputStream;
import java.time.Instant;
import java.util.Map;

public record DocumentSource(
        String documentId,
        String title,
        InputStream inputStream,
        String extension,     // "md", "json", "txt"
        String contentType,  // "text/markdown", "application/json"
        Map<String, Object> metadata,
        Instant lastModifiedAt
) {
}
