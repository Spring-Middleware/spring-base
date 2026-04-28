package io.github.spring.middleware.ai.rag.source;

import java.io.InputStream;
import java.time.Instant;
import java.util.Map;

public record DocumentSource(
        String documentId,
        String title,
        InputStream inputStream,
        Map<String, String> metadata,
        Instant lastModifiedAt
) {
}
