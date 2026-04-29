package io.github.spring.middleware.ai.infrastructure.rag.source.config;

import io.github.spring.middleware.ai.infrastructure.config.file.FileSystemDocumentSourceProviderProperties;
import io.github.spring.middleware.ai.infrastructure.config.mongo.MongoDocumentSourceProviderProperties;
import io.github.spring.middleware.ai.infrastructure.rag.source.file.FileSystemDocumentSourceProviderOptions;
import io.github.spring.middleware.ai.infrastructure.rag.source.mongo.MongoDocumentSourceProviderOptions;
import io.github.spring.middleware.ai.rag.source.DocumentSourceType;
import io.github.spring.middleware.ai.rag.source.config.DocumentSourceProviderOptions;
import lombok.Data;

@Data
public class DocumentSourceDefinition {

    private DocumentSourceType type;
    private String systemContext;
    private FileSystemDocumentSourceProviderProperties fileSystem;
    private MongoDocumentSourceProviderProperties mongo;


    public DocumentSourceProviderOptions optionsForType() {
        return switch (type) {
            case FILE_SYSTEM ->
                    fileSystem == null ? null : new FileSystemDocumentSourceProviderOptions(fileSystem.getPaths());
            case MONGO -> mongo == null ? null : new MongoDocumentSourceProviderOptions(mongo.getCollections());
            default -> throw new IllegalArgumentException(STR."Unsupported type: \{type}");
        };
    }

}
