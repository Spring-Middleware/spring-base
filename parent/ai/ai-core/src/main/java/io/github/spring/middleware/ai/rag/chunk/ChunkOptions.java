package io.github.spring.middleware.ai.rag.chunk;

public record ChunkOptions(int chunkSize, int chunkOverlap) {

    public ChunkOptions {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be greater than 0");
        }
        if (chunkOverlap < 0) {
            throw new IllegalArgumentException("chunkOverlap must be non-negative");
        }
        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("chunkOverlap must be less than chunkSize");
        }
    }
}
