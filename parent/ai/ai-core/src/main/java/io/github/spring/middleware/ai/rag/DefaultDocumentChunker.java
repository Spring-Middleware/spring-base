package io.github.spring.middleware.ai.rag;

import io.github.spring.middleware.ai.config.DocumentChunkerProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
public class DefaultDocumentChunker implements DocumentChunker {

    @Override
    public Flux<DocumentChunkInput> chunk(DocumentSource source, DocumentChunkerProperties properties) {

        return Flux.create(sink -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(source.inputStream(), StandardCharsets.UTF_8)
            )) {
                StringBuilder buffer = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null && !sink.isCancelled()) {
                    buffer.append(line).append('\n');

                    if (buffer.length() >= properties.getChunkSize()) {
                        String text = buffer.substring(0, Math.min(buffer.length(), properties.getChunkSize()));
                        sink.next(new DocumentChunkInput(text.trim(), source.metadata()));

                        int overlapStart = Math.max(0, buffer.length() - properties.getOverlapSize());
                        String overlap = buffer.substring(overlapStart);
                        buffer.setLength(0);
                        buffer.append(overlap);
                    }
                }

                if (!sink.isCancelled() && !buffer.isEmpty()) {
                    sink.next(new DocumentChunkInput(buffer.toString().trim(), source.metadata()));
                }

                sink.complete();
            } catch (Exception e) {
                sink.error(e);
            }
        });

    }

}
