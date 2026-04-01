# Declarative GraphQL Batching and Links

This document explains how Spring Middleware implements cross-service GraphQL resolution using the `@GraphQLLink` annotation family.

Spring Middleware does not treat GraphQL as a local concern inside a single service.  
It provides a **platform-level execution model** where the GraphQL Gateway composes queries, resolves linked fields, and coordinates execution across multiple microservices.

One of the most advanced capabilities of this model is **Declarative GraphQL Batching**.

---

## What this solves

In distributed GraphQL systems, the classic N+1 problem becomes a **network problem**.

Example:

- A query returns 100 `Catalog` items
- Each `Catalog` resolves `products`
- Each resolution triggers a remote call to `product-service`

Result:

```
100 catalogs → 100 remote GraphQL calls
```

This is not just inefficient — it introduces:

- network overhead
- latency amplification
- unnecessary load on downstream services

Spring Middleware solves this at the **platform level**, not at the resolver level.

---

## Declarative Batching

Declarative batching allows the gateway to:

- detect batchable links from metadata
- aggregate requests across multiple parent objects
- execute a single downstream query
- reconstruct results transparently

This happens without:

- manual `DataLoader` wiring
- resolver-specific batching logic
- custom orchestration code

Batching is driven entirely by annotations and gateway execution behavior.

---

## Configuration

Batching is enabled at the gateway level:

```yaml
graphql:
  gateway:
    batching:
      enabled: ${GRAPHQL_GATEWAY_BATCHING_ENABLED:true}
```

This allows:

- runtime enable/disable
- comparison of execution strategies
- safe rollout in production environments

---

## Batching Annotations

Batching is declared through link metadata:

- `@GraphQLLink(..., batched = true)`
- `@GraphQLLinkArgument(..., batch = true, targetFieldName = "...")`

Meaning:

- the link participates in batch execution
- multiple values are aggregated into a single argument
- results are matched back using `targetFieldName`

---

## Full Example: Catalog → Product

### Catalog Service (Domain)

```java
@GraphQLLink(
    schema = "product", 
    type = "Product", 
    query = "productsByIds", 
    collection = true, 
    batched = true,
    arguments = {
        @GraphQLLinkArgument(name = "ids", targetFieldName = "id", batch = true)
    }
)
private List<UUID> productIds;
```

This declares:

- a cross-service relationship
- a batched resolution strategy
- a mapping between local IDs and remote results

### Product Service (GraphQL Controller)

```java
@GraphQLQuery(name = "productsByIds")
public List<Product> getProductsByIds(@GraphQLArgument(name = "ids") List<UUID> ids) {
    return productService.getProductsByIds(ids);
}
```

The downstream service remains simple:

- no batching logic
- no gateway awareness
- no execution orchestration

---

## Execution Model

### Without batching

```
Catalog.products (100 items)
↓
100 remote calls → product-service
```

### With declarative batching

```
Catalog.products (100 items)
↓
Gateway aggregates IDs
↓
1 remote call → productsByIds(ids)
↓
Gateway reconstructs results
```

---

## How it works internally

At the GraphQL Gateway level:

1. **Field resolution starts**
    - Linked fields are detected from metadata

2. **Batch registration**
    - Requests are stored in a request-scoped registry

3. **Aggregation**
    - IDs are grouped across multiple parent objects

4. **Dispatch**
    - A single downstream GraphQL query is executed

5. **Reconstruction**
    - Results are matched using `targetFieldName`
    - Data is mapped back to each original parent object

This process is:

- transparent to services
- independent from resolver code
- driven entirely by metadata

---

## Platform-level responsibility

Spring Middleware makes a clear distinction:

| Concern | Responsibility |
|--------|----------------|
| Schema & resolvers | Service |
| Execution orchestration | Gateway |
| Batching strategy | Platform |

This allows:

- services to remain simple
- execution to remain consistent
- optimizations to be applied globally

---

## Basic GraphQL Links (Non-Batched)

Besides batching, the `@GraphQLLink` family supports:

- **Simple argument mapping**  
  Field value → single remote argument

- **Multi-argument mapping**  
  Method returns `Map` or `GraphQLLinkArguments`

Example:

```java
@GraphQLLink(
    schema = "product", 
    type = "Page_Product", 
    query = "products", 
    arguments = {
        @GraphQLLinkArgument(name = "catalogId"), 
        @GraphQLLinkArgument(name = "q")
    }
)
```

---

## Collection architecture in microservices

During startup:

1. `GraphQLLinkedTypeBuilder` scans annotated types
2. Builds `GraphQLFieldLinkDefinition`
3. Registers link metadata in the platform

---

## Gateway execution flow

At runtime:

1. Gateway intercepts linked field resolution
2. Delegates to `RemoteDelegatingGraphQLLinkDataFetcher`
3. Applies batching logic when enabled
4. Builds downstream query using:
    - `InLineFragmentBuilder`
    - `SelectionSetBuilder`
5. Executes remote call
6. Maps results back to parent entities

---

## Why this matters

Declarative batching changes where complexity lives.

Instead of:

- pushing complexity into resolvers
- duplicating logic across services

It centralizes execution in the platform:

- consistent behavior across all services
- fewer bugs and edge cases
- better performance by default

Most importantly:

> N+1 is solved at the platform level, not at the resolver level.

---

## Tips for microservice developers

- Use `@GraphQLLink` on fields for ID-based relationships
- Use method-based links for multi-argument scenarios
- Always combine with `@GraphQLQuery`
- Let the gateway handle batching and execution

---

## Related Links

- [GraphQL Overview](./graphql.md)