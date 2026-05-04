package io.github.spring.middleware.ai.infrastructure.rag.vector;

import io.github.spring.middleware.ai.rag.chunk.DocumentChunk;
import io.github.spring.middleware.ai.rag.utils.CosineSimilarity;
import io.github.spring.middleware.ai.rag.vector.SearchRequest;
import io.github.spring.middleware.ai.rag.vector.VectorNamespace;
import io.github.spring.middleware.ai.rag.vector.VectorStore;
import io.github.spring.middleware.ai.rag.vector.VectorType;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MongoVectorStore implements VectorStore {

    private final MongoTemplate mongoTemplate;

    public MongoVectorStore(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Void> add(VectorNamespace namespace, DocumentChunk chunk) {
        return Mono.fromRunnable(() -> mongoTemplate.save(chunk, namespace.value()));
    }

    @Override
    public Flux<DocumentChunk> search(SearchRequest request) {
        Criteria criteria = new Criteria();

        if (request.hasFilter()) {
            List<String> values = request.filterValues().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .toList();

            if (!values.isEmpty()) {
                String metadataField = request.filterField().startsWith("metadata.")
                        ? request.filterField()
                        : STR."metadata.\{request.filterField()}";

                switch (request.matchType()) {
                    case MATCH_ANY -> criteria = Criteria.where(metadataField).in(values);
                    case MATCH_ALL -> criteria = new Criteria().andOperator(
                            values.stream()
                                    .map(value -> Criteria.where(metadataField).is(value))
                                    .toArray(Criteria[]::new)
                    );
                }
            }
        }

        Query query = new Query(criteria);

        List<DocumentChunk> chunks = mongoTemplate.find(query, DocumentChunk.class, request.namespace().value());

        if (request.embedding() != null && !request.embedding().isEmpty()) {
            return Flux.fromIterable(chunks.stream()
                    .sorted(Comparator.comparingDouble(chunk ->
                            -CosineSimilarity.calculate(request.embedding(), chunk.embedding())
                    ))
                    .limit(request.topK())
                    .toList());
        } else {
            return Flux.fromIterable(chunks.stream()
                    .limit(request.topK())
                    .toList());
        }
    }

    public Mono<Boolean> exists(VectorNamespace namespace, String documentId, String embeddingModel, String checksum) {
        return Mono.fromSupplier(() ->
                mongoTemplate.exists(
                        Query.query(
                                Criteria.where("documentId").is(documentId)
                                        .and("embeddingModel").is(embeddingModel)
                                        .and("checksum").is(checksum)
                        ),
                        DocumentChunk.class,
                        namespace.value()
                )
        );
    }

    public Mono<Void> deleteByDocumentIdAndEmbeddingModelExceptChecksums(
            VectorNamespace namespace,
            String documentId,
            String embeddingModel,
            Set<String> checksums
    ) {
        return Mono.fromRunnable(() -> {
            mongoTemplate.remove(
                    Query.query(
                            Criteria.where("documentId").is(documentId)
                                    .and("embeddingModel").is(embeddingModel)
                                    .and("checksum").nin(checksums)
                    ),
                    DocumentChunk.class,
                    namespace.value()
            );
        });
    }


    @Override
    public VectorType getType() {
        return VectorType.MONGO;
    }
}
