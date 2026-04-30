package io.github.spring.middleware.ai.infrastructure.rag.source.file;

import io.github.spring.middleware.ai.infrastructure.rag.source.config.DocumentSourceProperties;
import io.github.spring.middleware.ai.rag.source.DocumentSource;
import io.github.spring.middleware.ai.rag.source.DocumentSourceProvider;
import io.github.spring.middleware.ai.rag.source.DocumentSourceType;
import io.github.spring.middleware.ai.rag.utils.PathUtils;
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
    public Flux<DocumentSource> load(String sourceName, @MonotonicNonNull FileSystemDocumentSourceProviderOptions options) {
        return Flux.fromIterable(options.paths())
                .flatMap(providedPath -> walkPath(sourceName, providedPath));
    }

    @Override
    public DocumentSourceType type() {
        return DocumentSourceType.FILE_SYSTEM;
    }

    private Flux<DocumentSource> walkPath(String sourceName, String providedPath) {
        Path startPath = Path.of(providedPath);
        if (!Files.exists(startPath)) {
            return Flux.empty();
        }

        return Flux.using(
                () -> Files.walk(startPath),
                stream -> Flux.fromStream(
                        stream.filter(Files::isRegularFile)
                                .map(path -> toDocumentSource(path, sourceName))
                ),
                Stream::close
        );
    }

    private DocumentSource toDocumentSource(Path path, String sourceName) {
        String extension = PathUtils.getExtension(path);
        try {
            return new DocumentSource(
                    path.toString(),
                    path.getFileName().toString(),
                    Files.newInputStream(path),
                    extension,
                    PathUtils.getContentType(path, extension),
                    Map.of(
                            "sourceType", "file-system",
                            "sourceName", sourceName,
                            "path", path.toString()
                    ),
                    Files.getLastModifiedTime(path).toInstant()
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


}
