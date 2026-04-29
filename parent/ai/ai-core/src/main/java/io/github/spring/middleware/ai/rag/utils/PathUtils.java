package io.github.spring.middleware.ai.rag.utils;

import java.nio.file.Files;
import java.nio.file.Path;

public final class PathUtils {

    private PathUtils() {
    }

    public static String getExtension(Path path) {
        String filename = path.getFileName().toString();
        int i = filename.lastIndexOf('.');
        return (i > 0) ? filename.substring(i + 1).toLowerCase() : "";
    }

    public static String getContentType(Path path, String extension) {
        String contentType = null;
        try {
            contentType = Files.probeContentType(path);
        } catch (Exception ex) {
            //  ignore
        }
        if (contentType == null) {
            contentType = guessFromExtension(extension);
        }
        return contentType;
    }

    private static String guessFromExtension(String ext) {
        return switch (ext) {
            case "md" -> "text/markdown";
            case "txt" -> "text/plain";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "html" -> "text/html";
            case "csv" -> "text/csv";
            default -> "application/octet-stream";
        };
    }
}
