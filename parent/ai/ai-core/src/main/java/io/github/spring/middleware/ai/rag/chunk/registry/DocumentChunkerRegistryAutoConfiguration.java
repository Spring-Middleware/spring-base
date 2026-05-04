package io.github.spring.middleware.ai.rag.chunk.registry;

import io.github.spring.middleware.ai.rag.chunk.DocumentChunker;
import io.github.spring.middleware.ai.rag.chunk.config.DocumentChunkerConfiguration;
import io.github.spring.middleware.ai.rag.chunk.deflt.DefaultDocumentChunker;
import io.github.spring.middleware.ai.rag.chunk.json.JsonChunker;
import io.github.spring.middleware.ai.rag.chunk.markdown.MarkdownChunker;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AutoConfiguration
public class DocumentChunkerRegistryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DocumentChunkerRegistry.class)
    public DocumentChunkerRegistry documentChunkerRegistry(
            List<DocumentChunker<?>> chunkersBeans,
            DocumentChunkerConfiguration config) {

        Map<String, DocumentChunker<?>> configuredChunkers = new HashMap<>();

        if (config.getDocumentChunkers() != null) {
            config.getDocumentChunkers().forEach((name, properties) -> {
                switch (properties.getType()) {
                    case DEFAULT:
                        chunkersBeans.stream()
                                .filter(c -> c instanceof DefaultDocumentChunker)
                                .findFirst()
                                .ifPresent(c -> configuredChunkers.put(name, c));
                        break;
                    case JSON:
                        chunkersBeans.stream()
                                .filter(c -> c instanceof JsonChunker)
                                .findFirst()
                                .ifPresent(c -> configuredChunkers.put(name, c));
                        break;
                    case MARKDOWN:
                        chunkersBeans.stream()
                                .filter(c -> c instanceof MarkdownChunker)
                                .findFirst()
                                .ifPresent(c -> configuredChunkers.put(name, c));
                        break;
                }
            });
        }

        // Add the beans themselves to fallback if needed
        chunkersBeans.forEach(c -> {
            String beanName = c.getClass().getSimpleName();
            beanName = Character.toLowerCase(beanName.charAt(0)) + beanName.substring(1);
            configuredChunkers.putIfAbsent(beanName, c);
        });

        return new DefaultDocumentChunkerRegistry(configuredChunkers);
    }

}
