package io.github.spring.middleware.ai.rag.chunk;

import io.github.spring.middleware.ai.rag.source.DocumentSource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class DefaultDocumentChunker implements DocumentChunker {

    @Override
    public Flux<DocumentChunkInput> chunk(DocumentSource source, ChunkOptions chunkOptions) {

        return Flux.create(sink -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(source.inputStream(), StandardCharsets.UTF_8)
            )) {
                StringBuilder buffer = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null && !sink.isCancelled()) {
                    buffer.append(line).append('\n');

                    if (buffer.length() >= chunkOptions.chunkSize()) {
                        String text = buffer.substring(0, Math.min(buffer.length(), chunkOptions.chunkSize()));
                        sink.next(new DocumentChunkInput(text.trim(), buildMetaData(source, chunkOptions)));

                        int overlapStart = Math.max(0, buffer.length() - chunkOptions.chunkOverlap());
                        String overlap = buffer.substring(overlapStart);
                        buffer.setLength(0);
                        buffer.append(overlap);
                    }
                }

                if (!sink.isCancelled() && !buffer.isEmpty()) {
                    sink.next(new DocumentChunkInput(buffer.toString().trim(), buildMetaData(source, chunkOptions)));
                }

                sink.complete();
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    private Map<String, String> buildMetaData(DocumentSource source, ChunkOptions chunkOptions) {
        Map metadata = new HashMap(source.metadata());
        metadata.put("chunkSize", String.valueOf(chunkOptions.chunkSize()));
        metadata.put("chunkOverlap", String.valueOf(chunkOptions.chunkOverlap()));
        return metadata;
    }

}
