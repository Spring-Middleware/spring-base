# Context Module: AI
## AI Infrastructure

Spring Middleware includes an AI module family designed to expose AI capabilities as framework infrastructure rather than application-specific chatbot code.

The goal is to allow developers to configure AI-backed capabilities without needing to understand the internal mechanics of:

- LLM provider routing
- embeddings
- chunking
- vector stores
- semantic retrieval
- Retrieval-Augmented Generation (RAG)
- prompt context construction

The developer-facing model should remain declarative and Spring Boot friendly.

### AI Module Structure

Current AI modules:

```text
ai
 ├─ ai-core
 ├─ ai-ollama
 ├─ ai-infrastructure
 └─ ai-boot
```

Responsibilities:

### ai-core

Defines provider-independent contracts and domain abstractions.

Main concepts:

- `AIClient`
- `ChatClient`
- `EmbeddingClient`
- `AIRequest`
- `ChatRequest`
- `EmbeddingRequest`
- `AIResponse`
- `ChatResponse`
- `EmbeddingResponse`
- `AIMessage`
- `AIRole`
- `Conversation`
- `ConversationClient`
- `AIProvider`
- `AIProviderRegistry`
- `ProviderChatClient`
- `ProviderEmbeddingClient`
- `DocumentSource`
- `DocumentSourceProvider`
- `DocumentSourceProviderProperties`
- `DocumentChunkInput`
- `DocumentChunker`
- `DocumentChunkerProperties`
- `DocumentIndexer`
- `DocumentChunk`
- `VectorStore`

### ai-ollama

Provides Ollama-specific provider adapters.

Responsibilities:

- chat completion through Ollama `/api/chat`
- embedding generation through Ollama embeddings API
- Ollama provider registration
- Ollama model support based on configured models
- Ollama HTTP configuration

Main components:

- `OllamaAIProvider`
- `OllamaAIProperties`
- `OllamaProviderChatClient`
- `OllamaProviderEmbeddingsClient`

Configuration prefix:

```text
middleware.ai.provider.ollama
```

Example configuration:

```yaml
middleware:
  ai:
    provider:
      ollama:
        models:
          - llama3.1:8b
          - nomic-embed-text
        base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
```

The provider name does not need to be repeated inside the provider block because the configuration namespace already identifies the provider as Ollama.

### ai-infrastructure

Provides concrete infrastructure implementations for AI storage, retrieval, and document sources.

Current and planned responsibilities:

- in-memory vector store
- file-system document source provider
- MongoDB document source provider
- future HTTP / REST / GraphQL document source provider
- future Redis, MongoDB, PostgreSQL pgvector, Qdrant, or external vector store implementations

The first stateful AI infrastructure component is the in-memory vector store.

Unlike `DefaultChatClient`, `DefaultEmbeddingClient`, or `AIProviderRegistry`, a vector store is real infrastructure because it stores indexed chunks and performs retrieval over persistent or in-memory state.

### ai-boot

Provides Spring Boot integration and higher-level AI services.

Current direction:

- documentation-aware chat service
- conversation lifecycle management
- in-memory conversation store
- RAG orchestration
- auto-configuration for document indexing and retrieval
- simple adapter/controller layer for starting conversations and asking follow-up questions

---

## AI Client Model

The AI client model follows the same routing pattern across capabilities.

Base contract:

```java
public interface AIClient<R extends AIRequest, S extends AIResponse> {

  S generate(R aiRequest);

}
```

Chat client:

```java
public interface ChatClient extends AIClient<ChatRequest, ChatResponse> {
}
```

Embedding client:

```java
public interface EmbeddingClient extends AIClient<EmbeddingRequest, EmbeddingResponse> {
}
```

The default clients do not call infrastructure directly.

They resolve an `AIProvider` through `AIProviderRegistry`, then delegate to the provider-specific client.

Conceptual flow:

```text
DefaultChatClient
  ↓
AIProviderRegistry.resolve(model)
  ↓
AIProvider.getChatClient()
  ↓
ProviderChatClient.generate(request)
```

Embedding flow:

```text
DefaultEmbeddingClient
  ↓
AIProviderRegistry.resolve(model)
  ↓
AIProvider.getEmbeddingClient()
  ↓
ProviderEmbeddingClient.generate(request)
```

Because this routing pattern is repeated across AI capabilities, the design may use a generic abstract routing client.

Conceptual model:

```java
public abstract class AbstractRoutingAIClient<
        R extends AIRequest,
        S extends AIResponse,
        C extends AIClient<R, S>
        > implements AIClient<R, S> {

  private final AIProviderRegistry registry;
  private final Function<AIProvider, C> clientResolver;

  protected AbstractRoutingAIClient(
          AIProviderRegistry registry,
          Function<AIProvider, C> clientResolver
  ) {
    this.registry = registry;
    this.clientResolver = clientResolver;
  }

  @Override
  public S generate(R request) {
    AIProvider provider = registry.resolve(request.getModel());
    C client = clientResolver.apply(provider);
    return client.generate(request);
  }
}
```

This keeps future AI capabilities consistent:

- chat
- embeddings
- image generation
- audio
- tool calling
- structured output

---

## AI Provider Model

AI providers expose provider-specific capabilities through a common abstraction.

Current provider model:

```java
public interface AIProvider {

  boolean supports(String model);

  ProviderChatClient getChatClient();

  ProviderEmbeddingClient getEmbeddingClient();

}
```

Provider support is currently model-driven.

The provider resolves whether it supports a model based on configuration.

Example:

```java
@Override
public boolean supports(String model) {
  if (!properties.isEnabled()) {
    return false;
  }

  if (model == null || model.isBlank()) {
    return false;
  }

  return properties.getModels()
          .stream()
          .anyMatch(m -> m.equalsIgnoreCase(model));
}
```

This keeps model routing explicit.

Default models are intentionally avoided for chat flows because the same conversation may switch between models.

The caller must pass the model explicitly.

Example:

```java
documentationChatService.ask(conversationId, "llama3.1:8b", "How does batching work?");
```

This enables:

- switching models within a conversation
- comparing models for the same context
- explicit provider routing
- no hidden default-model behavior

---

## AI Conversation Model

Spring Middleware includes a conversation model for multi-turn interactions.

The goal is to support stateful AI conversations while keeping model selection explicit.

Core concept:

```java
public interface ConversationClient {

  ChatResponse chat(Conversation conversation, String model, String userMessage);

}
```

A conversation stores:

- system messages
- user messages
- assistant messages
- accumulated conversation history

The `DocumentationChatService` should use the conversation model because documentation chat is naturally multi-turn.

Preferred API:

```java
public interface DocumentationChatService {

  DocumentationConversationResponse startConversation(String model, String question);

  ChatResponse ask(UUID conversationId, String model, String question);

}
```

Conversation lifecycle:

```text
startConversation(model, question)
  ↓
create Conversation
  ↓
add documentation system prompt
  ↓
send first question
  ↓
store Conversation by UUID
  ↓
return conversationId + first response
```

Follow-up flow:

```text
ask(conversationId, model, question)
  ↓
load Conversation from ConversationStore
  ↓
append user message
  ↓
call ConversationClient
  ↓
append assistant response
  ↓
return response
```

Initial storage strategy:

```java
public interface ConversationStore {

  UUID create(Conversation conversation);

  Conversation get(UUID conversationId);

  void remove(UUID conversationId);

}
```

First implementation:

```text
InMemoryConversationStore
```

Implementation detail:

- uses `ConcurrentHashMap<UUID, Conversation>`
- UUID is generated when the conversation starts
- controller/adapter returns the UUID to the caller
- follow-up requests use the UUID to continue the conversation

This allows an HTTP adapter model such as:

```text
POST /ai/documentation/conversations
  -> starts a conversation
  -> returns conversationId + first response

POST /ai/documentation/conversations/{conversationId}/messages
  -> asks a follow-up question in the same conversation
```

---

## AI Documentation Chat

The first concrete AI use case is a documentation assistant for Spring Middleware.

The purpose is not to build a generic chatbot.

The purpose is to allow the framework to answer questions using Spring Middleware documentation as external knowledge.

Initial service:

```java
public interface DocumentationChatService {

  DocumentationConversationResponse startConversation(String model, String question);

  ChatResponse ask(UUID conversationId, String model, String question);

}
```

The documentation assistant should:

- use `ConversationClient`
- add a documentation-specific system message
- retrieve relevant documentation chunks through RAG
- answer only using the provided Spring Middleware context
- avoid inventing APIs, annotations, endpoints, or behavior
- say that something is not documented if the answer is not present in context

The system message should establish behavior such as:

```text
You are the Spring Middleware documentation assistant.

Answer only using the Spring Middleware documentation context.
If the answer is not documented yet, say that it is not documented yet.

Be concise, technical, and do not invent APIs.
```

As the RAG pipeline evolves, the documentation context should be dynamically retrieved rather than hardcoded into the prompt.

---

## Embeddings and RAG

Spring Middleware AI is moving toward a real RAG model.

RAG flow:

```text
User question
  ↓
EmbeddingClient generates question embedding
  ↓
VectorStore searches similar document chunks
  ↓
topK chunks are converted into prompt context
  ↓
ConversationClient sends context + question to the LLM
  ↓
LLM answers using retrieved context
```

Important principle:

The LLM does not learn the documentation permanently.

Instead, the framework retrieves relevant context and sends it to the model at request time.

Embeddings are used to represent text as vectors.

Example:

```text
"GraphQL batching" -> [0.123, -0.982, 0.44, ...]
```

Semantic similarity is used to find chunks whose meaning is close to the user question.

Example:

```text
similarity("batching", "N+1") -> high
similarity("batching", "pizza") -> low
```

The first vector store implementation is in-memory.

Future vector store implementations may include:

- MongoDB Atlas Vector Search
- Redis
- PostgreSQL pgvector
- Qdrant
- Pinecone
- other vector databases

---

## Document Indexing Model

Spring Middleware AI introduces a declarative document indexing model.

The developer should not need to know how RAG works internally.

The framework should allow users to configure sources, and the framework should handle:

```text
sources
  ↓
chunks
  ↓
embeddings
  ↓
vector store
  ↓
retrieval
  ↓
prompt context
  ↓
LLM response
```

Core contracts:

```java
public interface DocumentSourceProvider<P extends DocumentSourceProviderProperties> {

  Flux<DocumentSource> load(P properties);

}
```

```java
public interface DocumentSourceProviderProperties {
}
```

```java
public interface DocumentChunker {

  Flux<DocumentChunkInput> chunk(
          DocumentSource source,
          DocumentChunkerProperties properties
  );

}
```

```java
public interface DocumentIndexer {

  Mono<Void> index(DocumentSource source, String embeddingModel);

  Mono<Void> index(Flux<DocumentSource> sources, String embeddingModel);

}
```

Document source:

```java
public record DocumentSource(
        String documentId,
        String title,
        InputStream inputStream,
        Map<String, String> metadata
) {
}
```

Chunk input:

```java
public record DocumentChunkInput(
        String content,
        Map<String, String> metadata
) {
}
```

Indexed chunk:

```java
public record DocumentChunk(
        UUID id,
        String documentId,
        String title,
        String content,
        List<Float> embedding,
        Map<String, String> metadata
) {
}
```

Vector store:

```java
public interface VectorStore {

  void add(DocumentChunk chunk);

  List<DocumentChunk> search(List<Float> embedding, int topK);

  void clear();

}
```

### Streaming Indexing

Document indexing should be streaming-oriented.

The chunker should not read an entire document into memory.

This is important because future document sources may contain very large documents.

Wrong model:

```text
InputStream -> String with full document -> List<DocumentChunkInput>
```

Preferred model:

```text
InputStream -> Flux<DocumentChunkInput> -> embedding per chunk -> VectorStore.add(chunk)
```

Reasoning:

- a document may be very large
- indexers should support files much larger than available heap
- chunks can be generated progressively
- chunks can be stored progressively
- the vector store may be in-memory today but persistent tomorrow

Conceptual indexing flow:

```java
@Override
public Mono<Void> index(DocumentSource source, String embeddingModel) {
  return documentChunker.chunk(source, properties)
          .concatMap(chunk ->
                  Mono.fromCallable(() -> embeddingClient.generate(
                                  new DefaultEmbeddingRequest(embeddingModel, chunk.content())
                          ))
                          .map(response -> new DocumentChunk(
                                  UUID.randomUUID(),
                                  source.documentId(),
                                  source.title(),
                                  chunk.content(),
                                  response.getEmbedding(),
                                  chunk.metadata()
                          ))
                          .doOnNext(vectorStore::add)
          )
          .then();
}
```

### Chunking Strategy

The first chunking strategy may be simple:

- read progressively from an `InputStream`
- accumulate text up to a configured chunk size
- emit a `DocumentChunkInput`
- keep overlap between chunks
- continue until stream end

Chunker properties may include:

- `chunkSize`
- `overlap`

The chunker should later improve to support:

- paragraph-aware splitting
- Markdown heading-aware splitting
- sentence-aware splitting
- metadata enrichment per chunk
- section name extraction
- nested document structures

---

## Document Source Providers

Document source providers allow the framework to index knowledge from different systems.

The goal is to make knowledge ingestion declarative.

Current and planned providers:

- file system
- MongoDB
- HTTP
- REST
- GraphQL

### File System Document Source Provider

The file system provider loads documents from configured directories.

Properties:

```java
@ConfigurationProperties("middleware.ai.document.source.file-system")
public class FileSystemDocumentSourceProviderProperties implements DocumentSourceProviderProperties {

  private List<String> directories = List.of("docs");

}
```

Conceptual YAML:

```yaml
middleware:
  ai:
    document:
      source:
        file-system:
          directories:
            - docs
            - README.md
```

The provider should support multiple directories.

Each path is walked independently.

Conceptual flow:

```text
directories[]
  ↓
Files.walk(...)
  ↓
regular files
  ↓
DocumentSource
```

### Mongo Document Source Provider

MongoDB can be used as a document source.

This is useful when the knowledge to index already lives in MongoDB collections.

The provider should not require users to create Java POJOs for every collection.

Instead, it may use `org.bson.Document`, which behaves like a dynamic map of fields.

Example Mongo document:

```json
{
  "_id": "123",
  "title": "GraphQL Gateway",
  "content": "This module handles batching..."
}
```

Mongo source configuration:

```java
@ConfigurationProperties("middleware.ai.document.source.mongo")
public class MongoDocumentSourceProviderProperties implements DocumentSourceProviderProperties {

  private List<DocumentCollection> collections;

  @Data
  public static class DocumentCollection {
    private String collection;
    private String idField;
    private String titleField;
    private String contentField;
  }
}
```

Conceptual YAML:

```yaml
middleware:
  ai:
    document:
      source:
        mongo:
          collections:
            - collection: documentation
              id-field: _id
              title-field: title
              content-field: content
            - collection: reviews
              id-field: _id
              title-field: productName
              content-field: reviewText
```

Provider behavior:

```text
configured collections
  ↓
mongoTemplate.findAll(Document.class, collection)
  ↓
extract id/title/content fields
  ↓
convert content to InputStream
  ↓
DocumentSource
```

A Mongo source provider should support multiple collections.

Each collection may map different fields to:

- document id
- title
- content

This enables indexing collections such as:

- documentation
- product descriptions
- reviews
- support tickets
- knowledge base articles

A field-based Mongo provider works best when the content is plain text.

For complex nested structures, future support may include:

- nested paths such as `review.text`
- arrays
- custom extractors
- typed mappers

### HTTP / REST / GraphQL Document Source Provider

A future HTTP document source provider should allow indexing knowledge from remote APIs.

This includes REST endpoints and GraphQL queries.

The conceptual model:

```text
HTTP request
  ↓
response
  ↓
extractor / JSON path mapper
  ↓
DocumentSource
```

Possible configuration:

```yaml
middleware:
  ai:
    document:
      source:
        http:
          endpoints:
            - name: reviews
              url: http://localhost:8080/graphql
              method: POST
              content-type: application/json
              body: |
                {
                  "query": "query { reviews { id productName text } }"
                }
              id-path: data.reviews[].id
              title-path: data.reviews[].productName
              content-path: data.reviews[].text
```

This provider should support use cases such as:

- indexing GraphQL query results
- indexing REST resource responses
- indexing external documentation APIs
- indexing product descriptions from service endpoints
- indexing user reviews from an API

A custom extractor may also be useful:

```java
public interface HttpDocumentSourceExtractor {

  Flux<DocumentSource> extract(HttpDocumentSourceResponse response);

}
```

This keeps the provider flexible for complex response shapes.

---

## RAG Use Cases

RAG should complement structured service calls, not replace them.

For structured operational data such as:

```text
catalogs -> products -> reviews
```

GraphQL batching remains the correct execution model.

GraphQL is responsible for:

- entity fetching
- ID-based relationships
- pagination
- filters
- consistency
- typed schema execution
- distributed joins through links

RAG is useful for semantic questions over textual or semi-structured content.

Examples:

```text
Which products have recurring battery complaints?
Summarize negative reviews for this catalog.
What issues appear most often in reviews for physical products?
Find products similar to this description.
What does the documentation say about GraphQL batching?
```

Example chunk for review-oriented RAG:

```text
Product: iPhone 15
Catalog: Mobile Deals
Review: Battery is terrible and drains quickly...
```

Metadata:

```text
catalogId = ...
productId = ...
type = review
```

The result of RAG retrieval may identify relevant chunks.

Then structured APIs such as GraphQL can still fetch the real entities by ID.

Principle:

```text
RAG = semantic retrieval and contextual answering
GraphQL = structured entity retrieval and relationships
```

---

## Recent Improvements (2026-04)

### RAG Context Builder

Spring Middleware introduces a **RAG Context Builder** component responsible for constructing prompt context from indexed document chunks.

Main concepts:

- `RagContextBuilder`
- `RagContextRequest`
- `RagContext`
- `DefaultRagContextBuilder`

Responsibilities:

- generate embeddings from user queries using `EmbeddingClient`
- resolve the correct `VectorStore` through `VectorStoreRegistry`
- retrieve topK relevant `DocumentChunk` entries
- format chunks into LLM-friendly prompt context
- return both raw chunks and formatted context

Conceptual flow:

```text
User question
  ↓
EmbeddingClient
  ↓
VectorStore.search()
  ↓
DocumentChunk[]
  ↓
formatting
  ↓
RagContext
```

Important design decisions:

- context generation is **stateless**
- context is **not persisted in Conversation**
- context is injected per request
- conversation history remains clean (user + assistant only)

This allows:

- deterministic prompt construction
- no context accumulation across turns
- no prompt pollution
- easier debugging and reproducibility

### Conversation Context Isolation

RAG context is applied at request time without mutating the underlying conversation state.

Pattern:

```text
Conversation (persisted)
  ↓ copy()
Temporary Conversation
  ↓ add augmented user message (context + question)
LLM call
  ↓
Persist only clean user + assistant messages
```

This ensures:

- separation of concerns between memory and retrieval
- no duplication of context across turns
- consistent conversation size
- safe multi-turn interactions

### Document Indexing Improvements

Document indexing has been extended to support incremental updates and reindexing strategies.

Enhancements:

- checksum-based deduplication of `DocumentChunk`
- detection of existing chunks through `VectorStore.exists(...)`
- deletion of outdated chunks using:

```java
deleteByDocumentIdAndEmbeddingModelExceptChecksums(...)
```

This allows:

- reindexing when chunking strategy changes
- safe updates when document content changes
- avoidance of duplicate embeddings
- consistent index state

### Chunking Configuration Awareness

Chunk identity may depend not only on content but also on indexing configuration.

Recommended approach:

- include chunking parameters in indexing identity:
  - `chunkSize`
  - `overlap`
  - `embeddingModel`

This ensures:

- changes in chunking strategy trigger reindexing
- old chunks are invalidated automatically
- vector store consistency is preserved

### Chunking Strategy Evolution

Current chunking:

- generic streaming chunker
- fixed-size chunks with overlap

Observed limitations:

- word truncation
- broken code blocks
- poor semantic boundaries

Planned improvements:

- Markdown-aware chunking
- JSON-aware chunking
- structure-aware splitting (headings, sections, paragraphs)
- pluggable `DocumentChunker` implementations
- user-defined chunking strategies

### RAG Limitations and Next Steps

Current limitations:

- no similarity score threshold filtering
- possible retrieval of weakly relevant chunks
- no semantic validation of context relevance

Planned improvements:

- scored retrieval (`ScoredDocumentChunk`)
- similarity threshold filtering
- fallback to "Not documented yet" when no relevant context is found
- improved prompt instructions for relevance handling

### Document Source Expansion

Future document source providers will support:

- REST APIs with pagination
- GraphQL queries with pagination
- parameterized requests for iterative data retrieval

Conceptual model:

```text
request(page=1)
  ↓
response
  ↓
extract documents
  ↓
request(page=2)
  ↓
...
```

This enables:

- indexing large remote datasets
- integrating external knowledge sources
- continuous ingestion pipelines

---

# Context Maintenance Rules

When the user asks to **add something to the context** ("añadir al contexto"), the following rules apply:

- The assistant must return the **entire context document**, not only the added section.
- Existing sections **must not be modified, reordered, or removed** unless explicitly requested.
- New information should be **appended in the most relevant section** or added as a new subsection.
- The goal is to **extend the context while preserving stability of the document structure**.
- The resulting document must remain **fully copy-paste safe**.

Additional clarification:

- The assistant **must not rewrite existing explanations**, even if they could be improved.
- The assistant **must not refactor section names or headings** unless explicitly requested.
- The assistant **must not collapse or summarize sections** of the document.
- The assistant **must treat this document as a stable knowledge base**, extending it incrementally instead of regenerating it.

---

# Documentation Output Rules

When generating Markdown documentation for this project:

- Always return the document inside a fenced block using `~~~~markdown` instead of ` ```markdown `.
- This avoids breaking nested code blocks that contain triple backticks, for example XML, YAML, JSON, or Java examples.
- The content inside the block must be valid Markdown and safe to copy directly into `.md` files.
- This rule applies whenever documentation is requested for:
  - README files
  - architecture documentation
  - AI_CONTEXT updates
  - examples or guides
  - any `.md` content

Additional clarification:

- The assistant **must never replace parts of the document with placeholders** such as:
  - `[...]`
  - `[... contenido ...]`
  - `[... SIN MODIFICAR ...]`
- The assistant must always return the **full explicit content**, even if sections are unchanged.
~~~~markdown