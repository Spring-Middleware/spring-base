package io.github.spring.middleware.ai.rag.utils;

public final class ContentUtils {

    private ContentUtils() {
    }

    public static String inferContentType(String content) {
        String trimmed = content.trim();

        if (trimmed.startsWith("#") || trimmed.contains("\n#")) {
            return "text/markdown";
        }

        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return "application/json";
        }

        if (trimmed.startsWith("<")) {
            return "application/xml";
        }

        return "text/plain";
    }

    public static String mapExtension(String contentType) {
        return switch (contentType) {
            case "text/markdown" -> "md";
            case "application/json" -> "json";
            case "application/xml" -> "xml";
            case "text/plain" -> "txt";
            default -> "bin";
        };
    }

}
