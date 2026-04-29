package io.github.spring.middleware.ai.rag.chunk;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.spring.middleware.ai.rag.chunk.deflt.ChunkOptions;
import io.github.spring.middleware.ai.rag.chunk.markdown.MarkdownChunkerOptions;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ChunkOptions.class, name = "default"),
    @JsonSubTypes.Type(value = MarkdownChunkerOptions.class, name = "markdown")
})
public interface ChunkerOptions {
}
