# Spring Middleware AI Module

The **Spring Middleware AI Module** provides a **declarative infrastructure for integrating LLMs and Retrieval-Augmented Generation (RAG)** into distributed systems.

Instead of building ad-hoc pipelines, the module allows developers to define how data becomes knowledge using configuration-driven rules.

---

## 🧠 Core Idea

```text
Raw Data (JSON, Markdown, APIs)
→ Declarative Transformation (Chunking Rules)
→ Semantic Chunks (Text + Metadata)
→ Embeddings
→ Vector Store (RAG)
```

The key principle is:

> **Separate the ingestion engine from the business-specific knowledge modeling.**

---

## 🧱 Module Structure

- **`ai-core`**
    - Core abstractions: Chat, Embeddings, Document, Chunking, RAG pipeline
    - Interfaces: `AIClient`, `EmbeddingClient`, `DocumentChunker`, etc.

- **`ai-ollama`**
    - LLM provider integration (local/self-hosted via Ollama)

- **`ai-infrastructure`**
    - Integrations with vector stores and data sources
    - Designed to work with systems like Qdrant

- **`ai-boot`**
    - Spring Boot auto-configuration
    - Property-driven setup of AI components

---

## 📥 Data Sources

RAG pipelines start with **Document Sources**.

The module exposes `DocumentSourceProvider`, allowing ingestion from:

- File systems (Markdown, JSON)
- Databases (MongoDB, SQL)
- APIs (REST/GraphQL)
- Custom sources

These sources feed the **Document Indexer**, which delegates to the **Chunking Engine**.

---

## ✂️ Chunking Engine

Chunking is where raw data becomes **LLM-friendly knowledge**.

Spring Middleware provides multiple strategies:

### Markdown Chunking

- Splits by headings
- Preserves document hierarchy
- Optimized for documentation ingestion

---

## 🔥 JSON Semantic Chunking (Key Feature)

Unlike traditional RAG systems that do:

```text
JSON → stringify → embeddings ❌
```

Spring Middleware does:

```text
JSON → semantic modeling → natural language → embeddings ✅
```

---

## 🧠 JSON Chunker Overview

The JSON Chunker is a **declarative transformation engine** that:

- Extracts structured data using JsonPath
- Builds semantic context using templates
- Preserves relationships (parent → child)
- Produces:
    - Text (for embeddings)
    - Metadata (for filtering)

---

## ⚙️ Rule-Based Configuration

Chunking is defined via YAML rules.

These rules act as a **DSL (Domain-Specific Language)** for transforming JSON into knowledge.

---

## 🧩 Rule Structure

```yaml
rules:
  - name: catalog
    extractor-path: "$.data.catalogs.content[*]"
```

Each rule defines:

- `extractor-path`: JsonPath selector
- `extractor-rules`: field extraction
- `generation-text-rules`: text generation
- `children`: hierarchical processing

---

## 🧪 Extraction Model

Each extractor defines how data is used:

```yaml
- json-data-types: [FIELD, META_DATA]
  name: catalogId
  extractor-path: "$.id"
```

### Data Types

- **FIELD**
    - Available for templates
    - Inherited by child rules (context propagation)

- **META_DATA**
    - Stored in vector DB payload
    - Used for filtering queries

---

## 🧠 Important Design Principle

```text
FIELD     → semantic context
META_DATA → query filters
TEXT      → (removed, replaced by templates)
```

Text is **not automatically generated** anymore.  
All output is explicitly controlled via templates.

---

## 🧾 Template Engine

Text is generated using declarative templates:

```yaml
- template: "Product {productName} belongs to catalog {catalogName}."
  variables:
    productName: productName!
    catalogName: catalogName
```

---

## ⚙️ Template Features

### 1. Required Variables (`!`)

```yaml
productName: productName!
```

- Mandatory field
- If missing → template is skipped

---

### 2. Fallback Values (`:"value"`)

```yaml
productType: productType:"product"
```

- Uses default if field is missing
- Avoids broken sentences

---

### 3. Optional Variables

```yaml
catalogName: catalogName
```

- Replaced with empty string if missing
- Template still renders if meaningful

---

### 4. Smart Rendering Rules

A template is rendered only if:

- All required variables are present
- At least one variable has a value

This avoids noise like:

```text
Product  belongs to catalog .
```

---

## 🔗 Hierarchical Context (Key Feature)

Rules can be nested:

```text
catalog
  → product
```

Parent fields are **inherited as FIELD only**:

```text
catalogName → available in product templates
catalog metadata → NOT inherited automatically
```

This enables:

```text
"Product X belongs to catalog Y"
```

without duplicating data.

---

## 🧾 Example

### Input (JSON)

```json
{
  "name": "Sunglasses",
  "price": { "amount": 25.5 }
}
```

### Output (Chunk)

```text
Product Sunglasses belongs to catalog Summer Collection.
It is a PhysicalProduct with price 25.5 USD.
```

### Metadata

```json
{
  "chunkType": "product",
  "productName": "Sunglasses",
  "catalogName": "Summer Collection"
}
```

---

## 🔍 Metadata & Filtering (Vector Search)

Metadata is stored alongside embeddings and used for filtering.

Example with Qdrant:

```json
{
  "must": [
    { "key": "chunkType", "match": { "value": "product" } },
    { "key": "catalogName", "match": { "value": "Summer Collection" } }
  ]
}
```

This enables:

```text
semantic search + structured filtering
```

---

## ⚙️ Configuration

Enable chunkers via `application.yml`:

```yaml
spring:
  ai:
    rag:
      document-chunker:
        json:
          enabled: true
        markdown:
          enabled: true
```

External rule files:

```yaml
document-chunker:
  json:
    catalogs:
      rules-path: classpath:rag/json/catalogs-rules.yml
```

---

## 🚀 Key Advantages

- No Java code required for new data sources
- Declarative knowledge modeling
- Context-aware chunking (hierarchical)
- Clean separation: engine vs business rules
- Better embeddings (semantic text, not raw JSON)
- First-class metadata filtering

---

## 🧠 Final Insight

Most RAG systems do:

```text
data → text → embeddings
```

Spring Middleware does:

```text
data → model → context → semantic text → embeddings
```

👉 This is the difference between **basic RAG** and **high-quality retrieval systems**.