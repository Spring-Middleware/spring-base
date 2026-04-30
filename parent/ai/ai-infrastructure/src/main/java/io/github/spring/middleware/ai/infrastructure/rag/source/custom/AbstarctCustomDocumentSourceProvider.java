package io.github.spring.middleware.ai.infrastructure.rag.source.custom;

import io.github.spring.middleware.ai.rag.source.DocumentSource;
import io.github.spring.middleware.ai.rag.source.DocumentSourceProvider;
import io.github.spring.middleware.ai.rag.source.DocumentSourceType;
import reactor.core.publisher.Flux;

public abstract class AbstarctCustomDocumentSourceProvider implements DocumentSourceProvider<CustomDocumentSourceProviderOptions> {

    public Flux<DocumentSource> load(String sourceName, CustomDocumentSourceProviderOptions options) {
        return loadSource(sourceName, options);
    }

    protected abstract Flux<DocumentSource> loadSource(String sourceName, CustomDocumentSourceProviderOptions options);

    @Override
    public DocumentSourceType type() {
        return DocumentSourceType.CUSTOM;
    }

    protected String getStringProperty(
            CustomDocumentSourceProviderOptions options,
            String name
    ) {
        return getProperty(options, name, String.class);
    }

    protected <T> T getProperty(
            CustomDocumentSourceProviderOptions options,
            String name,
            Class<T> clazz
    ) {
        Object object = options.properties().get(name);

        if (clazz.isInstance(object)) {
            return clazz.cast(object);
        }

        throw new IllegalArgumentException(
                STR."Property '\{name}' is not of type \{clazz.getSimpleName()} or is missing"
        );
    }

    protected <T> T getPropertyOrDefault(
            CustomDocumentSourceProviderOptions options,
            String name,
            Class<T> clazz,
            T defaultValue
    ) {
        Object object = options.properties().get(name);

        if (object == null) {
            return defaultValue;
        }

        if (clazz.isInstance(object)) {
            return clazz.cast(object);
        }

        throw new IllegalArgumentException(
                STR."Property '\{name}' is not of type \{clazz.getSimpleName()}"
        );
    }

}
