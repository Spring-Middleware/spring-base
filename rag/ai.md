# Spring Middleware AI Module

The **Spring Middleware AI Module** provides a **declarative infrastructure for integrating LLMs and Retrieval-Augmented Generation (RAG)** into distributed systems.

It is designed to let developers **model knowledge, not pipelines**.

---

## 🧠 Core Idea

```text
Raw Data (JSON, Markdown, APIs)
→ Declarative Transformation (Chunking Rules)
→ Semantic Chunks (Text + Metadata)
→ Embeddings
→ Vector Store (RAG)
→ Query Planning + Retrieval
→ LLM Response
```

The key principle:

> **Separate infrastructure, knowledge modeling, and query understanding.**

---

## 🧱 Module Structure

- **`ai-core`**
  - Core abstractions: Chat, Embeddings, Chunking, RAG
  - Reactive contracts (`Mono` / `Flux`)
  - Interfaces: `AIClient`, `ChatClient`, `EmbeddingClient`, `VectorStore`, etc.

- **`ai-ollama`**
  - Local/self-hosted LLM provider via `ai-ollama`
  - Chat + embeddings

- **`ai-infrastructure`**
  - Vector stores (Qdrant, in-memory, future: pgvector, Redis…)
  - Document sources (file system, MongoDB, APIs)

- **`ai-boot`**
  - Spring Boot auto-configuration
  - RAG orchestration
  - Chat APIs

---

## ⚡ Fully Reactive by Design

The entire AI pipeline is **non-blocking**:

- `ChatClient.generate → Mono<ChatResponse>`
- `EmbeddingClient.generate → Mono<EmbeddingResponse>`
- `VectorStore.search → Flux<DocumentChunk>`

### Rule:

```text
No .block() inside the pipeline. Ever.
```

Blocking providers (LLMs, embeddings) must be isolated:

```java
Mono.fromCallable(() -> embeddingClient.generate(...))
        .subscribeOn(Schedulers.boundedElastic());
```

---

## 📥 Data Sources

RAG starts from **Document Sources**:

- Markdown documentation
- JSON APIs (REST / GraphQL)
- MongoDB collections
- Custom providers

Each source feeds the **Document Indexer**, which runs the chunking pipeline.

---

## ✂️ Chunking Engine

Chunking transforms raw data into **LLM-consumable knowledge**.

### Supported strategies:

- Markdown (headings-based)
- JSON (semantic modeling)
- Custom chunkers

---

## 🔥 JSON Semantic Chunking (Key Feature)

Most systems do:

```text
JSON → stringify → embeddings ❌
```

Spring Middleware does:

```text
JSON → semantic modeling → natural language → embeddings ✅
```

This is **the core differentiator**.

---

## 🧠 JSON Chunker

The JSON chunker is a **declarative transformation engine**:

- Extracts data using JsonPath
- Builds semantic text using templates
- Preserves relationships (catalog → product → reviews)
- Produces:
  - **Text** → for embeddings
  - **Metadata** → for filtering

---

## ⚙️ Rule-Based Configuration (DSL)

Rules define how data becomes knowledge:

```yaml
rules:
  - name: catalog
    extractor-path: "$.data.catalogs.content[*]"
```

Each rule supports:

- extraction
- transformation
- hierarchy

---

## 🧪 Extraction Model

```yaml
- json-data-types: [FIELD, META_DATA]
  name: catalogId
  extractor-path: "$.id"
```

### Data Types

- **FIELD**
  - Used in templates
  - Inherited by children

- **META_DATA**
  - Stored in vector DB
  - Used for filtering

---

## 🧠 Core Principle

```text
FIELD     → semantic context
META_DATA → exact filtering
TEXT      → explicitly generated
```

---

## 🧾 Template Engine

```yaml
- template: "Product {productName} belongs to catalog {catalogName}."
  variables:
    productName: productName!
    catalogName: catalogName
```

### Features

- `!` → required
- `:"value"` → fallback
- optional variables supported
- smart rendering (no empty sentences)

---

## 🔗 Hierarchical Context

Rules can be nested:

```text
catalog
  → product
```

Inheritance:

- FIELD → inherited
- META_DATA → NOT inherited (explicit design)

Enables:

```text
"Product X belongs to catalog Y"
```

without duplication.

---

## 🧾 Example

### Input

```json
{
  "name": "Sunglasses",
  "price": { "amount": 25.5 }
}
```

### Output

```text
Product Sunglasses belongs to catalog Summer Collection.
It costs 25.5 USD.
```

### Metadata

```json
{
  "productName": "Sunglasses",
  "catalogName": "Summer Collection",
  "chunkType": "product"
}
```

---

## 🔍 Retrieval Model (RAG)

### Flow

```text
User query
→ Query Planner
→ Embedding
→ Vector search (+ filters)
→ topK chunks
→ Prompt context
→ LLM
```

---

## 🧠 Query Planner (Critical Layer)

The planner converts user intent into a **search plan**.

### Responsibilities

- extract filters
- detect semantic vs exact search
- normalize values
- generate optimized query

---

### 🔑 Key Rules

#### 1. Exact values → metadata

```text
IDs, SKU, codes → FILTERS ONLY
```

❌ Wrong:
```text
"find DPT-000006" via embeddings
```

✅ Correct:
```json
{
  "field": "productSku",
  "values": ["DPT-000006"]
}
```

---

#### 2. Natural language → semantic

```text
"productos similares a iPhone 15"
```

→ semantic search

---

#### 3. Similarity queries

- NO productName filter
- use semantic search

---

#### 4. Metadata normalization

Stored values are **canonical (English)**:

```text
"físico" → "PHYSICAL"
"digital" → "DIGITAL"
```

---

## 🧠 Important Insight

> **Embeddings are bad at exact matching. Metadata is not optional.**

If you don’t model metadata correctly:

- SKU queries fail
- ID lookups fail
- filters break
- results look random

---

## 🗄️ Vector Store

Reactive contract:

```java
Mono<Void> add(...)
Flux<DocumentChunk> search(...)
Mono<Boolean> exists(...)
Mono<Void> delete(...)
```

### Example: Metadata filter

```json
{
  "must": [
    { "key": "productType", "match": { "value": "PHYSICAL" } }
  ]
}
```

---

## 🚀 Indexing Pipeline

```text
DocumentSource
→ Chunker (Flux)
→ exists check
→ embedding
→ vector store
→ cleanup
```

Pattern:

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

---

## ⚠️ Common Pitfalls

### 1. Blocking in reactive flow

```text
.block() → ❌ runtime error
```

---

### 2. Forgetting subscription

```text
Mono created but never executed → ❌
```

---

### 3. Using semantic search for IDs

```text
SKU / ID via embeddings → ❌ garbage results
```

---

### 4. Poor chunk design

If chunks don’t include relationships:

```text
Product → Catalog
```

LLM cannot reconstruct answers.

---

## 🚀 Key Advantages

- Fully declarative (no code for new sources)
- Reactive by design
- Strong separation of concerns:
  - ingestion
  - modeling
  - retrieval
  - generation
- High-quality embeddings (semantic, not raw)
- First-class metadata filtering
- Pluggable providers and vector stores

---

## 🧠 Final Insight

Most RAG systems:

```text
data → text → embeddings
```

Spring Middleware:

```text
data → model → context → semantic text → embeddings → controlled retrieval
```

👉 This is the difference between:

- **“It kind of works” RAG**
- and
- **production-grade retrieval systems**

