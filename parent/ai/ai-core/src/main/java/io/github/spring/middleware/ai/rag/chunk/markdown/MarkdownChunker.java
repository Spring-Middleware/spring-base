package io.github.spring.middleware.ai.rag.chunk.markdown;

import io.github.spring.middleware.ai.rag.chunk.ChunkerSuitability;
import io.github.spring.middleware.ai.rag.chunk.DocumentChunkInput;
import io.github.spring.middleware.ai.rag.chunk.DocumentChunker;
import io.github.spring.middleware.ai.rag.source.DocumentSource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MarkdownChunker implements DocumentChunker<MarkdownChunkerOptions> {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$");

    @Override
    public Flux<DocumentChunkInput> chunk(DocumentSource source, MarkdownChunkerOptions options) {
        return Flux.create(sink -> {
            ParsingStatus parsingStatus = new ParsingStatus();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(source.inputStream(), StandardCharsets.UTF_8))) {

                List<String> headingPath = new ArrayList<>();
                StringBuilder current = new StringBuilder();

                int sectionIndex = 0;
                String line;

                while ((line = reader.readLine()) != null) {

                    if (isHeading(line, parsingStatus, options)) {
                        emitSection(sink, source, current, headingPath, sectionIndex++, options, parsingStatus);
                        updateHeadingPath(headingPath, line);

                        if (options.includeHeadingInChunk()) {
                            current.append(line).append("\n");
                        }
                    } else {
                        current.append(line).append("\n");
                    }

                    updateCodeBlockState(line, parsingStatus);
                }

                emitSection(sink, source, current, headingPath, sectionIndex, options, parsingStatus);
                sink.complete();

            } catch (Exception ex) {
                sink.error(ex);
            }
        });
    }

    private boolean isHeading(
            String line,
            ParsingStatus parsingStatus,
            MarkdownChunkerOptions options
    ) {
        if (parsingStatus.isInsideCodeBlock()) {
            return false;
        }

        Matcher matcher = HEADING.matcher(line);

        if (!matcher.matches()) {
            return false;
        }

        int level = matcher.group(1).length();

        return level <= options.maxHeadingLevel();
    }

    private void updateHeadingPath(
            List<String> headingPath,
            String line
    ) {
        Matcher matcher = HEADING.matcher(line);

        if (!matcher.matches()) {
            return;
        }

        int level = matcher.group(1).length();
        String title = matcher.group(2).trim();

        while (headingPath.size() >= level) {
            headingPath.remove(headingPath.size() - 1);
        }

        headingPath.add(title);
    }

    private void emitSection(
            FluxSink<DocumentChunkInput> sink,
            DocumentSource source,
            StringBuilder current,
            List<String> headingPath,
            int sectionIndex,
            MarkdownChunkerOptions options,
            ParsingStatus parsingStatus
    ) {
        String sectionText = current.toString().trim();

        if (sectionText.isBlank()) {
            current.setLength(0);
            return;
        }

        String prefix = parsingStatus.consumePendingPrefix();

        if (!prefix.isBlank()) {
            sectionText = prefix + "\n\n" + sectionText;
        }

        if (sectionText.length() < options.minChars()) {
            parsingStatus.appendPendingPrefix(sectionText);
            current.setLength(0);
            return;
        }

        int partIndex = 0;

        for (String chunkText : splitSection(sectionText, options)) {
            sink.next(toChunk(
                    source,
                    chunkText,
                    headingPath,
                    sectionIndex,
                    partIndex++
            ));
        }

        current.setLength(0);
    }

    private List<String> splitSection(String text, MarkdownChunkerOptions options) {
        int maxChars = Math.max(options.maxChars(), 1);

        if (text.length() <= maxChars) {
            return List.of(text);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + maxChars, text.length());

            if (end < text.length() && options.splitByParagraphs()) {
                int paragraphBreak = text.lastIndexOf("\n\n", end);

                if (paragraphBreak > start) {
                    end = paragraphBreak;
                }
            }

            String chunk = text.substring(start, end).trim();

            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            start = end;
        }

        return chunks;
    }

    private DocumentChunkInput toChunk(
            DocumentSource source,
            String content,
            List<String> headingPath,
            int sectionIndex,
            int partIndex
    ) {

        String sectionName = headingPath.isEmpty()
                ? source.title()
                : headingPath.get(headingPath.size() - 1);

        String sectionPath = headingPath.isEmpty()
                ? source.title()
                : String.join(" > ", headingPath);

        Map<String, Object> metadata = new HashMap<>();

        // metadata propia del chunker
        metadata.put("chunker", "markdown");
        metadata.put("sourceId", source.documentId());
        metadata.put("sourceName", source.title());
        metadata.put("headingPath", List.copyOf(headingPath));
        metadata.put("sectionName", sectionName);
        metadata.put("sectionPath", sectionPath);
        metadata.put("sectionIndex", sectionIndex);
        metadata.put("partIndex", partIndex);
        metadata.put("contentType", source.contentType());
        metadata.put("extension", source.extension());

        // 🔥 aquí metes la metadata del source
        if (source.metadata() != null) {
            metadata.putAll(source.metadata());
        }

        return new DocumentChunkInput(
                content,
                Map.copyOf(metadata) // lo haces inmutable al final
        );
    }



    private void updateCodeBlockState(String line, ParsingStatus parsingStatus) {
        if (line.trim().startsWith("```")) {
            parsingStatus.flipInsideCodeBlock();
        }
    }

    @Override
    public int suitability(DocumentSource source) {
        boolean markdownExtension =
                "md".equalsIgnoreCase(source.extension())
                        || "markdown".equalsIgnoreCase(source.extension());

        boolean markdownContentType =
                "text/markdown".equalsIgnoreCase(source.contentType());

        if (markdownExtension && markdownContentType) {
            return ChunkerSuitability.EXACT_MATCH;
        }

        if (markdownContentType) {
            return ChunkerSuitability.CONTENT_TYPE_MATCH;
        }

        if (markdownExtension) {
            return ChunkerSuitability.EXTENSION_MATCH;
        }

        return ChunkerSuitability.UNSUPPORTED;
    }

    @Override
    public Class<MarkdownChunkerOptions> optionsType() {
        return MarkdownChunkerOptions.class;
    }

    @Override
    public List<String> getMetadataFields(String sourceNane) {

        Set<String> fields = new LinkedHashSet<>();

        // campos base del markdown chunker
        fields.addAll(List.of(
                "chunker",
                "sourceId",
                "sourceName",
                "headingPath",
                "sectionName",
                "sectionPath",
                "sectionIndex",
                "partIndex",
                "contentType",
                "extension"
        ));

        return List.copyOf(fields);
    }

    private static class ParsingStatus {

        private boolean insideCodeBlock;
        private String pendingPrefix = "";

        boolean isInsideCodeBlock() {
            return insideCodeBlock;
        }

        void flipInsideCodeBlock() {
            this.insideCodeBlock = !this.insideCodeBlock;
        }

        String consumePendingPrefix() {
            String value = pendingPrefix;
            pendingPrefix = "";
            return value;
        }

        void appendPendingPrefix(String text) {
            if (!pendingPrefix.isBlank()) {
                pendingPrefix += "\n\n";
            }
            pendingPrefix += text;
        }

    }
}