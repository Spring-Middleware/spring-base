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

---

## AI Provider Model

AI providers expose provider-specific capabilities through a common abstraction.

```java
public interface AIProvider {

  boolean supports(String model);

  ProviderChatClient getChatClient();

  ProviderEmbeddingClient getEmbeddingClient();

}
```

---

## AI Conversation Model

Spring Middleware includes a conversation model for multi-turn interactions.

```java
public interface ConversationClient {

  ChatResponse chat(Conversation conversation, String model, String userMessage);

}
```

ConversationStore:

```java
public interface ConversationStore {

  UUID create(Conversation conversation);

  Conversation get(UUID conversationId);

  void remove(UUID conversationId);

}
```

---

## Embeddings and RAG

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

---

## Document Indexing Model

Core contracts:

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

}
```

---

## Streaming Indexing

Preferred model:

```text
InputStream -> Flux<DocumentChunkInput> -> embedding per chunk -> VectorStore.add(chunk)
```

---

## Recent Improvements (2026-04)

### RAG Context Builder
(unchanged)

### Conversation Context Isolation
(unchanged)

### Document Indexing Improvements
(unchanged)

---

## Recent Improvements (2026-05)

### Fully Reactive Vector Store Integration

Vector store operations have been migrated to a fully reactive model.

Updated contract:

```java
public interface VectorStore {

  Mono<Void> add(VectorNamespace namespace, DocumentChunk chunk);

  Flux<DocumentChunk> search(VectorNamespace namespace, List<Float> embedding, int topK);

  Mono<Boolean> exists(VectorNamespace namespace, String documentId, String embeddingModel, String checksum);

  Mono<Void> deleteByDocumentIdAndEmbeddingModelExceptChecksums(
          VectorNamespace namespace,
          String documentId,
          String embeddingModel,
          Set<String> checksums
  );

}
```

Key principles:

- no blocking calls (`.block()`) inside reactive pipelines
- all I/O operations return `Mono` or `Flux`
- operations must be composed using Reactor operators

### Reactive Indexing Pipeline

The indexing flow is now fully reactive:

```text
DocumentSource
  ↓
DocumentChunker (Flux)
  ↓
VectorStore.exists (Mono<Boolean>)
  ↓
EmbeddingClient (blocking → wrapped in boundedElastic)
  ↓
VectorStore.add (Mono<Void>)
  ↓
cleanup (delete outdated chunks)
```

Key pattern:

```java
.concatMap(chunk ->
    vectorStore.exists(...)
        .flatMap(exists -> {
            if (exists) return Mono.empty();

            return Mono.fromCallable(() -> embeddingClient.generate(...))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(vectorStore::add);
        })
)
```

### Blocking Isolation Strategy

External blocking operations (LLM / embedding providers) must be isolated:

```java
Mono.fromCallable(() -> embeddingClient.generate(...))
    .subscribeOn(Schedulers.boundedElastic());
```

Rules:

- only wrap blocking calls
- never wrap the whole pipeline
- keep vector store operations non-blocking

### Lazy Execution and Mono.defer

Reactive pipelines are lazy.

Common pitfall:

```java
.then(vectorStore.delete(...)) // WRONG
```

Correct approach:

```java
.then(Mono.defer(() -> vectorStore.delete(...)))
```

Reason:

- ensures execution happens after upstream completes
- prevents usage of stale mutable state

### Idempotent Collection Management (Qdrant)

Vector stores must handle collection creation safely.

Pattern:

```java
webClient.put(...)
  .retrieve()
  .toBodilessEntity()
  .onErrorResume(WebClientResponseException.Conflict.class, e -> Mono.empty());
```

Additionally:

```java
ConcurrentMap<String, Mono<Void>> cache = new ConcurrentHashMap<>();

cache.computeIfAbsent(collection, key ->
    ensureCollection(...).cache()
);
```

Benefits:

- avoids race conditions
- ensures single creation per namespace
- prevents redundant network calls

### JSON Chunker Improvements

JSON chunker now supports both:

- array-based extraction
- object-based extraction

Problem:

```java
List<Object> nodes = document.read(path); // not always a list
```

Solution:

```java
Object selected = document.read(path);

Iterable<Object> iterable =
    selected instanceof Iterable ? (Iterable<Object>) selected : List.of(selected);
```

This enables:

- paginated responses (`content[*]`)
- single-entity responses (`$`)

### Metadata Inheritance

Child rules can inherit metadata from parent rules.

Previous behavior:

- only FIELD values inherited

Current behavior:

- META_DATA can also be inherited
- enables cross-level filtering (e.g. product filtered by catalogName)

### Reactive Execution Pitfalls

Common mistakes:

- calling `.block()` inside reactive flow → runtime error
- calling `Mono` without subscribing → nothing executes
- using `doOnNext` for side effects that should be reactive → incorrect

Correct pattern:

```java
.flatMap(vectorStore::add)
```

instead of:

```java
.doOnNext(vectorStore::add)
```

---

# Context Maintenance Rules
(unchanged)

# Documentation Output Rules
(unchanged)
~~~~markdown