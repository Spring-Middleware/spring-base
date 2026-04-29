package io.github.spring.middleware.ai.rag.chunk;

import java.util.Map;

public record DocumentChunkInput(
        String content,
        Map<String, Object> metadata
) {

    public String getEmbeddingContent() {

        String sourceName = getString("sourceName");
        String sectionPath = getString("sectionPath");

        StringBuilder builder = new StringBuilder();

        if (sourceName != null) {
            builder.append("Document: ").append(sourceName).append("\n");
        }

        if (sectionPath != null && !sectionPath.isBlank()) {
            builder.append("Section: ").append(sectionPath).append("\n");
        }

        if (builder.length() > 0) {
            builder.append("\n"); // separación clara
        }

        builder.append(content());

        return builder.toString();
    }

    private String getString(String key) {
        Object value = metadata().get(key);
        return value != null ? value.toString() : null;
    }
}
