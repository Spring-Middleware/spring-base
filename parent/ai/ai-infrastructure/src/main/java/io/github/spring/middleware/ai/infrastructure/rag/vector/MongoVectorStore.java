package io.github.spring.middleware.ai.infrastructure.rag.vector;

import io.github.spring.middleware.ai.rag.chunk.DocumentChunk;
import io.github.spring.middleware.ai.rag.utils.CosineSimilarity;
import io.github.spring.middleware.ai.rag.vector.VectorNamespace;
import io.github.spring.middleware.ai.rag.vector.VectorStore;
import io.github.spring.middleware.ai.rag.vector.VectorType;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class MongoVectorStore implements VectorStore {

    private final MongoTemplate mongoTemplate;

    public MongoVectorStore(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void add(VectorNamespace namespace, DocumentChunk chunk) {
        mongoTemplate.save(chunk, namespace.value());
    }

    @Override
    public List<DocumentChunk> search(VectorNamespace namespace, List<Float> embedding, int topK) {
        return mongoTemplate.findAll(DocumentChunk.class, namespace.value())
                .stream()
                .sorted(Comparator.comparingDouble(chunk ->
                        -CosineSimilarity.calculate(embedding, chunk.embedding())
                ))
                .limit(topK)
                .toList();
    }

    public boolean exists(VectorNamespace namespace, String documentId, String embeddingModel, String checksum) {
        return mongoTemplate.exists(
                Query.query(
                        Criteria.where("documentId").is(documentId)
                                .and("embeddingModel").is(embeddingModel)
                                .and("checksum").is(checksum)
                ),
                DocumentChunk.class,
                namespace.value()
        );
    }

    public void deleteByDocumentIdAndEmbeddingModelExceptChecksums(
            VectorNamespace namespace,
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
                namespace.value()
        );
    }


    @Override
    public VectorType getType() {
        return VectorType.MONGO;
    }
}
