package io.github.spring.middleware.ai.rag.index;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public abstract class AbstractDocumentIndexerOptions implements DocumentIndexerOptions {

    private final String embeddingModel;

}
