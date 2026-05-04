package io.github.spring.middleware.ai.rag.chunk.deflt;

import io.github.spring.middleware.ai.rag.chunk.ChunkerSuitability;
import io.github.spring.middleware.ai.rag.chunk.DocumentChunkInput;
import io.github.spring.middleware.ai.rag.chunk.DocumentChunker;
import io.github.spring.middleware.ai.rag.source.DocumentSource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DefaultDocumentChunker implements DocumentChunker<ChunkOptions> {

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

    @Override
    public int suitability(DocumentSource source) {
        return ChunkerSuitability.FALLBACK;
    }

    @Override
    public Class<ChunkOptions> optionsType() {
        return ChunkOptions.class;
    }

    @Override
    public List<String> getMetadataFields(String sourceName) {

        Set<String> fields = new LinkedHashSet<>();

        // campos base del default chunker
        fields.addAll(List.of(
                "chunker",
                "chunkSize",
                "chunkOverlap"
        ));


        return List.copyOf(fields);
    }

    private Map<String, Object> buildMetaData(DocumentSource source, ChunkOptions chunkOptions) {

        Map<String, Object> metadata = new HashMap<>();

        if (source.metadata() != null) {
            metadata.putAll(source.metadata());
        }

        metadata.put("chunker", "default");
        metadata.put("chunkSize", chunkOptions.chunkSize());
        metadata.put("chunkOverlap", chunkOptions.chunkOverlap());

        return metadata;
    }

}
