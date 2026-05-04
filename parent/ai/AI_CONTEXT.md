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

---

## AI Module Structure

Current AI modules:

```text
ai
 ├─ ai-core
 ├─ ai-ollama
 ├─ ai-infrastructure
 └─ ai-boot
```

---

## ai-core

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
- `ConversationStore`
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
- `VectorNamespace`

---

## ai-ollama

Provides Ollama-specific provider adapters.

Responsibilities:

- chat completion through Ollama `/api/chat`
- embedding generation through Ollama embeddings API
- Ollama provider registration
- model resolution
- HTTP configuration

Main components:

- `OllamaAIProvider`
- `OllamaAIProperties`
- `OllamaProviderChatClient`
- `OllamaProviderEmbeddingsClient`

Configuration prefix:

```text
middleware.ai.provider.ollama
```

Example:

```yaml
middleware:
  ai:
    provider:
      ollama:
        models:
          - qwen2.5:7b-instruct
          - nomic-embed-text
        base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
```

---

## ai-infrastructure

Provides infrastructure implementations for storage, retrieval, and document ingestion.

Responsibilities:

- Vector store implementations
    - in-memory
    - Qdrant (current focus)
    - future: Redis, MongoDB, pgvector
- Document source providers
    - file-system
    - MongoDB
    - custom (REST / GraphQL APIs)
- Chunking strategies
    - Markdown
    - JSON
    - custom chunkers

---

## ai-boot

Provides Spring Boot integration and higher-level services.

Responsibilities:

- RAG orchestration
- documentation-aware chat services
- conversation lifecycle management
- in-memory conversation store
- indexing auto-configuration
- REST controllers for chat APIs

---

# AI Client Model (Reactive)

The AI client model is now fully reactive.

```java
public interface AIClient<R extends AIRequest, S extends AIResponse> {

  Mono<S> generate(R aiRequest);

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

Routing flow:

```text
DefaultChatClient
  ↓
AIProviderRegistry.resolve(model)
  ↓
AIProvider.getChatClient()
  ↓
ProviderChatClient.generate(request) -> Mono<ChatResponse>
```

---

## Abstract Routing Client

```java
public abstract class AbstractRoutingAIClient<
        R extends AIRequest,
        S extends AIResponse,
        C extends AIClient<R, S>
        > implements AIClient<R, S> {

  private final AIProviderRegistry registry;
  private final Function<AIProvider, C> clientResolver;

  @Override
  public Mono<S> generate(R request) {
    AIProvider provider = registry.resolve(request.getModel());
    C client = clientResolver.apply(provider);
    return client.generate(request);
  }
}
```

---

# AI Provider Model

```java
public interface AIProvider {

  boolean supports(String model);

  ProviderChatClient getChatClient();

  ProviderEmbeddingClient getEmbeddingClient();

}
```

---

# Conversation Model (Reactive)

```java
public interface ConversationClient {

  Mono<ChatResponse> chat(
      Conversation conversation,
      String model,
      String userMessage,
      String context
  );

}
```

```java
public interface ConversationStore {

  UUID create(Conversation conversation);

  Conversation get(UUID conversationId);

  void remove(UUID conversationId);

}
```

---

# RAG Architecture

## Core Flow

```text
User question
  ↓
Query Planner (optional metadata filters + optimized query)
  ↓
EmbeddingClient generates embedding
  ↓
VectorStore search (semantic + filters)
  ↓
topK chunks
  ↓
Context builder (prompt)
  ↓
ChatClient (LLM)
  ↓
Answer
```

---

## Query Planner Layer

The RAG pipeline includes a **query planner** responsible for:

- extracting metadata filters
- deciding semantic vs exact search
- normalizing metadata values
- generating optimized queries

### Key Rules

- **Exact identifiers (IDs, SKU, codes)** → MUST use metadata filters
- **Natural language queries** → semantic search
- **Similarity queries** → semantic only (no productName filter)
- **Metadata values normalized to English**

Example:

```json
{
  "optimizedQuery": "productos similares a iPhone 15",
  "filters": [],
  "useSemanticSearch": true
}
```

Example (exact SKU):

```json
{
  "optimizedQuery": "catalog containing product with sku DPT-000006",
  "filters": [
    {
      "field": "productSku",
      "values": ["DPT-000006"],
      "matchType": "MATCH_ANY"
    }
  ],
  "useSemanticSearch": false
}
```

---

# Vector Store (Reactive)

```java
public interface VectorStore {

  Mono<Void> add(VectorNamespace namespace, DocumentChunk chunk);

  Flux<DocumentChunk> search(
      VectorNamespace namespace,
      List<Float> embedding,
      int topK
  );

  Mono<Boolean> exists(
      VectorNamespace namespace,
      String documentId,
      String embeddingModel,
      String checksum
  );

  Mono<Void> deleteByDocumentIdAndEmbeddingModelExceptChecksums(
      VectorNamespace namespace,
      String documentId,
      String embeddingModel,
      Set<String> checksums
  );

}
```

---

# Reactive Indexing Pipeline

```text
DocumentSource
  ↓
DocumentChunker (Flux)
  ↓
VectorStore.exists
  ↓
EmbeddingClient (blocking wrapped)
  ↓
VectorStore.add
  ↓
cleanup
```

Pattern:

```java
.concatMap(chunk ->
    vectorStore.exists(...)
        .flatMap(exists -> {
            if (exists) return Mono.empty();

            return Mono.fromCallable(() -> embeddingClient.generate(...))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(response -> vectorStore.add(...));
        })
)
```

---

# Blocking Isolation

```java
Mono.fromCallable(() -> embeddingClient.generate(...))
    .subscribeOn(Schedulers.boundedElastic());
```

Rules:

- isolate ONLY blocking calls
- never block (`.block()`) inside reactive flow
- keep pipeline non-blocking

---

# Lazy Execution

Incorrect:

```java
.then(vectorStore.delete(...))
```

Correct:

```java
.then(Mono.defer(() -> vectorStore.delete(...)))
```

---

# Qdrant Integration

## Idempotent Collection Creation

```java
webClient.put(...)
  .retrieve()
  .toBodilessEntity()
  .onErrorResume(WebClientResponseException.Conflict.class, e -> Mono.empty());
```

## Cached Initialization

```java
ConcurrentMap<String, Mono<Void>> cache = new ConcurrentHashMap<>();

cache.computeIfAbsent(collection, key ->
    ensureCollection(...).cache()
);
```

---

# Chunking Model

## JSON Chunker

Supports:

- arrays (`content[*]`)
- objects (`$`)

```java
Object selected = document.read(path);

Iterable<Object> iterable =
    selected instanceof Iterable
        ? (Iterable<Object>) selected
        : List.of(selected);
```

---

# Metadata Strategy

## Key Principle

RAG quality depends more on **metadata design** than embeddings.

### Required metadata (recommended)

- `productId`
- `productName`
- `productSku`
- `productType` (PHYSICAL / DIGITAL)
- `catalogId`
- `catalogName`
- `catalogStatus`

### Important Rules

- IDs / SKU / codes → ALWAYS metadata filters
- embeddings are not reliable for exact lookup
- chunk content must include relational context

Example:

```text
Product Developer Productivity Toolkit (sku: DPT-000006)
belongs to catalog Developer Gear Collection #023.
```

---

# Known Limitations of RAG

- Cannot compute global aggregations (e.g. "most expensive product")
- Limited by `topK` retrieval
- May hallucinate if context is weak
- Semantic search does not work for identifiers

---

# Design Philosophy

Spring Middleware AI aims to:

- abstract complexity into infrastructure
- provide declarative configuration
- separate concerns:
    - planner (query understanding)
    - retriever (vector store)
    - generator (LLM)
- make RAG predictable and controllable

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