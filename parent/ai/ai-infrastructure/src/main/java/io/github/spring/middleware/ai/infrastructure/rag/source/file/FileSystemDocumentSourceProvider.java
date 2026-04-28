package io.github.spring.middleware.ai.infrastructure.rag.source.file;

import io.github.spring.middleware.ai.rag.source.DocumentSource;
import io.github.spring.middleware.ai.rag.source.DocumentSourceProvider;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

@Component
@Qualifier("fileSystemDocumentSourceProvider")
public class FileSystemDocumentSourceProvider implements DocumentSourceProvider<FileSystemDocumentSourceProviderOptions> {


    @Override
    public Flux<DocumentSource> load(@MonotonicNonNull FileSystemDocumentSourceProviderOptions options) {
        return Flux.fromIterable(options.paths())
                .flatMap(this::walkPath);
    }

    private Flux<DocumentSource> walkPath(String providedPath) {
        Path startPath = Path.of(providedPath);
        if (!Files.exists(startPath)) {
            return Flux.empty();
        }

        return Flux.using(
                () -> Files.walk(startPath),
                stream -> Flux.fromStream(
                        stream.filter(Files::isRegularFile)
                                .map(this::toDocumentSource)
                ),
                Stream::close
        );
    }

    private DocumentSource toDocumentSource(Path path) {
        try {
            return new DocumentSource(
                    path.toString(),
                    path.getFileName().toString(),
                    Files.newInputStream(path),
                    Map.of(
                            "source", "file-system",
                            "path", path.toString()
                    ),
                    Files.getLastModifiedTime(path).toInstant()
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
