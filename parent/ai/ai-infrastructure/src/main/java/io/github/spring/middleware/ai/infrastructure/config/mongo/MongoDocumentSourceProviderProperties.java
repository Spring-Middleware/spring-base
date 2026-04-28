package io.github.spring.middleware.ai.infrastructure.config.mongo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@Data
@ConfigurationProperties("middleware.ai.document.source.mongo")
public class MongoDocumentSourceProviderProperties {

    private List<DocumentCollection> collections;

    @Data
    public static class DocumentCollection {
        private String collection;
        private String idField = "_id";
        private String titleField = "title";
        private String contentField = "content";
        private String lastUpdateAtField = "lastUpdateAtField";
    }
}

