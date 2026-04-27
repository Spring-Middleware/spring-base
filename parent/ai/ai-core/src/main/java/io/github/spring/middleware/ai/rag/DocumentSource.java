package io.github.spring.middleware.ai.rag;

import java.io.InputStream;
import java.util.Map;

public record DocumentSource(
        String documentId,
        String title,
        InputStream inputStream,
        Map<String, String> metadata
) {
}
