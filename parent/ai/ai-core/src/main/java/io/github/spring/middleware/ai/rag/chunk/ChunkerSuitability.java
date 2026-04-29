package io.github.spring.middleware.ai.rag.chunk;

public final class ChunkerSuitability {

    public static final int UNSUPPORTED = 0;
    public static final int FALLBACK = 10;
    public static final int EXTENSION_MATCH = 70;
    public static final int CONTENT_TYPE_MATCH = 90;
    public static final int EXACT_MATCH = 100;

    private ChunkerSuitability() {
    }
}
