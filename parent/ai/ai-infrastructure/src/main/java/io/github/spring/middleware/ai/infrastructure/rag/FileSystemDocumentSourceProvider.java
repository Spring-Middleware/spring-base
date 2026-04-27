package io.github.spring.middleware.ai.infrastructure.rag;

import io.github.spring.middleware.ai.infrastructure.config.FileSystemDocumentSourceProviderProperties;
import io.github.spring.middleware.ai.rag.DocumentSource;
import io.github.spring.middleware.ai.rag.DocumentSourceProvider;
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
public class FileSystemDocumentSourceProvider implements DocumentSourceProvider<FileSystemDocumentSourceProviderProperties> {


    @Override
    public Flux<DocumentSource> load(FileSystemDocumentSourceProviderProperties properties) {
        return Flux.fromIterable(properties.getDirectories())
                .flatMap(this::walkDirectory);
    }

    private Flux<DocumentSource> walkDirectory(String dir) {
        return Flux.using(
                () -> Files.walk(Path.of(dir)),
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
                    Map.of("path", path.toString())
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
