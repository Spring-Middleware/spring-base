package io.github.spring.middleware.ai.infrastructure.rag;

import io.github.spring.middleware.ai.exception.AIErrorCodes;
import io.github.spring.middleware.ai.exception.AIException;
import io.github.spring.middleware.ai.infrastructure.config.MongoDocumentSourceProviderProperties;
import io.github.spring.middleware.ai.rag.DocumentSource;
import io.github.spring.middleware.ai.rag.DocumentSourceProvider;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Qualifier("mongoDocumentSourceProvider")
public class MongoDocumentSourceProvider implements DocumentSourceProvider<MongoDocumentSourceProviderProperties> {

    private final MongoTemplate mongoTemplate;

    public MongoDocumentSourceProvider(
            MongoTemplate mongoTemplate
    ) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<DocumentSource> load(MongoDocumentSourceProviderProperties properties) {

        return Flux.fromIterable(properties.getCollections())
                .flatMap(this::loadCollection);
    }

    private Flux<DocumentSource> loadCollection(MongoDocumentSourceProviderProperties.DocumentCollection collectionConfig) {

        return Flux.fromIterable(
                mongoTemplate.findAll(Document.class, collectionConfig.getCollection())
        ).map(doc -> toDocumentSource(doc, collectionConfig));
    }

    private DocumentSource toDocumentSource(
            Document doc,
            MongoDocumentSourceProviderProperties.DocumentCollection config
    ) {

        String id = getRequiredField(doc, config.getIdField());
        String title = getRequiredField(doc, config.getTitleField());
        String content = getRequiredField(doc, config.getContentField());

        return new DocumentSource(
                id,
                title,
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                Map.of(
                        "source", "mongo",
                        "collection", config.getCollection()
                )
        );
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
