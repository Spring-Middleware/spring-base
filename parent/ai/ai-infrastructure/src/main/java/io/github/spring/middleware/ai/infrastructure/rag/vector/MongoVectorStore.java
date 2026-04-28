package io.github.spring.middleware.ai.infrastructure.rag.vector;

import io.github.spring.middleware.ai.rag.chunk.DocumentChunk;
import io.github.spring.middleware.ai.rag.vector.CosineSimilarity;
import io.github.spring.middleware.ai.rag.vector.VectorStore;
import io.github.spring.middleware.ai.rag.vector.VectorType;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class MongoVectorStore implements VectorStore {

    private static final String COLLECTION = "middleware_ai_document_chunks";

    private final MongoTemplate mongoTemplate;

    public MongoVectorStore(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void add(DocumentChunk chunk) {
        mongoTemplate.save(chunk, COLLECTION);
    }

    @Override
    public List<DocumentChunk> search(List<Float> embedding, int topK) {
        return mongoTemplate.findAll(DocumentChunk.class, COLLECTION)
                .stream()
                .sorted(Comparator.comparingDouble(chunk ->
                        -CosineSimilarity.calculate(embedding, chunk.embedding())
                ))
                .limit(topK)
                .toList();
    }

    public boolean exists(String documentId, String embeddingModel, String checksum) {
        return mongoTemplate.exists(
                Query.query(
                        Criteria.where("documentId").is(documentId)
                                .and("embeddingModel").is(embeddingModel)
                                .and("checksum").is(checksum)
                ),
                DocumentChunk.class,
                COLLECTION
        );
    }

    public void deleteByDocumentIdAndEmbeddingModelExceptChecksums(
            String documentId,
            String embeddingModel,
            Set<String> checksums
    ) {
        mongoTemplate.remove(
                Query.query(
                        Criteria.where("documentId").is(documentId)
                                .and("embeddingModel").is(embeddingModel)
                                .and("checksum").nin(checksums)
                ),
                DocumentChunk.class,
                COLLECTION
        );
    }


    @Override
    public VectorType getType() {
        return VectorType.MONGO;
    }
}
