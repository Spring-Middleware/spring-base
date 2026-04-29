package io.github.spring.middleware.ai.rag.utils;

import java.util.List;

public final class CosineSimilarity {

    private CosineSimilarity() {
    }

    public static double calculate(List<Float> a, List<Float> b) {
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
