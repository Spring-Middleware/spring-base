package io.github.spring.middleware.ai.infrastructure.rag;

import io.github.spring.middleware.ai.rag.DocumentChunk;
import io.github.spring.middleware.ai.rag.VectorStore;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryVectorStore implements VectorStore {

    private final List<DocumentChunk> chunks = new CopyOnWriteArrayList<>();


    @Override
    public void add(DocumentChunk chunk) {
        chunks.add(chunk);
    }

    @Override
    public List<DocumentChunk> search(List<Float> embedding, int topK) {
        return chunks.stream()
                .sorted(Comparator.comparingDouble(chunk ->
                        -cosineSimilarity(embedding, chunk.embedding())
                ))
                .limit(topK)
                .toList();
    }



    private double cosineSimilarity(List<Float> a, List<Float> b) {
        if (a == null || b == null || a.size() != b.size()) {
            return 0.0;
        }

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.size(); i++) {
            float x = a.get(i);
            float y = b.get(i);

            dot += x * y;
            normA += x * x;
            normB += y * y;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
