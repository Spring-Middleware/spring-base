package io.github.spring.middleware.ai.infrastructure.rag.source.mongo;

import io.github.spring.middleware.ai.exception.AIErrorCodes;
import io.github.spring.middleware.ai.exception.AIException;
import io.github.spring.middleware.ai.infrastructure.config.mongo.MongoDocumentSourceProviderProperties.DocumentCollection;
import io.github.spring.middleware.ai.rag.source.DocumentSource;
import io.github.spring.middleware.ai.rag.source.DocumentSourceProvider;
import io.github.spring.middleware.ai.rag.source.DocumentSourceType;
import io.github.spring.middleware.ai.rag.utils.ContentUtils;
import org.bson.Document;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
@Qualifier("mongoDocumentSourceProvider")
public class MongoDocumentSourceProvider implements DocumentSourceProvider<MongoDocumentSourceProviderOptions> {

    private final MongoTemplate mongoTemplate;

    public MongoDocumentSourceProvider(
            MongoTemplate mongoTemplate
    ) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<DocumentSource> load(String sourceName, @MonotonicNonNull MongoDocumentSourceProviderOptions options) {
        if (options.collections() == null || options.collections().isEmpty()) {
            return Flux.empty();
        }
        return Flux.fromIterable(options.collections())
                .flatMap(collectionConfig -> loadCollection(sourceName, collectionConfig));
    }

    @Override
    public DocumentSourceType type() {
        return DocumentSourceType.MONGO;
    }

    private Flux<DocumentSource> loadCollection(String sourceName, DocumentCollection collectionConfig) {

        return Flux.fromIterable(
                mongoTemplate.findAll(Document.class, collectionConfig.getCollection())
        ).map(doc -> toDocumentSource(sourceName, doc, collectionConfig));
    }

    private DocumentSource toDocumentSource(
            String sourceName,
            Document doc,
            DocumentCollection config
    ) {

        String id = getRequiredField(doc, config.getIdField());
        String title = getRequiredField(doc, config.getTitleField());
        String content = getRequiredField(doc, config.getContentField());
        Instant lastModifiedAt = getLastUpdateAt(doc, config);

        String contentType = ContentUtils.inferContentType(content);
        return new DocumentSource(
                id,
                title,
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                ContentUtils.mapExtension(contentType),
                contentType,
                Map.of(
                        "sourceType", "mongo",
                        "sourceName", sourceName,
                        "collection", config.getCollection(),
                        "mongo.id", id,
                        "content.field", config.getContentField()
                ),
                lastModifiedAt
        );
    }


    private Instant getLastUpdateAt(Document doc, DocumentCollection config) {
        Object lastModifiedRaw = doc.get(config.getLastUpdateAtField());

        Instant lastModifiedAt;

        if (lastModifiedRaw instanceof Date date) {
            lastModifiedAt = date.toInstant();
        } else if (lastModifiedRaw instanceof String str) {
            lastModifiedAt = Instant.parse(str);
        } else {
            throw new IllegalStateException(STR."Unsupported lastModifiedAt type: \{lastModifiedRaw}");
        }
        return lastModifiedAt;
    }

    private String getRequiredField(Document doc, String field) {
        Object value = doc.get(field);
        if (value == null) {
            throw new AIException(
                    AIErrorCodes.INVALID_AI_REQUEST,
                    "Field '%s' not found in document".formatted(field)
            );
        }
        return value.toString();
    }

}
