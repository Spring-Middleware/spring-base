# Declarative GraphQL Batching and Links (RAG-Friendly)

## Quick Answer

**How do I solve the N+1 execution problem in distributed GraphQL federation?**
Enable gateway batching in `application.yml` and define a Declarative Link on your GraphQL model using `@GraphQLLink` and `@GraphQLLinkArgument` with `batch=true`.

**Server Java Code (Domain Model):**
```java
// Defining the link to batch requests against the target product service
import io.github.spring.middleware.annotation.graphql.GraphQLLink;
import io.github.spring.middleware.annotation.graphql.GraphQLLinkArgument;

@GraphQLLink(
    schema = "product", 
    type = "Product", 
    query = "productsByIds", 
    collection = true, 
    batched = true,  // Activates the GraphQL gateway batching feature
    arguments = {
        @GraphQLLinkArgument(name = "ids", targetFieldName = "id", batch = true)
    }
)
private List<UUID> productIds;
```

**YAML Configuration:**
```yaml
graphql:
  gateway:
    batching:
      enabled: ${GRAPHQL_GATEWAY_BATCHING_ENABLED:true}
```

**Constraints:**
- The `@GraphQLLinkArgument` parameter MUST define `batch=true`.
- You MUST specify a `targetFieldName` to dynamically map back the query results efficiently at the GraphQL Gateway level.
- The downstream GraphQL service remains unaware of any batching orchestration.

---

## Execution Model

### How does Declarative Batching work internally?
The Gateway transparently aggregates execution logic:
1. It registers the queries into a request-scoped registry.
2. It groups `IDs` across multiple parent objects.
3. Dispatches just *one* downstream GraphQL query iteratively (e.g., `productsByIds(ids)`).
4. Restructures the mapped values securely using `targetFieldName`.

**Performance Scenario:**
Instead of 100 `Catalog` elements querying `product-service` 100 separate times incrementally, the gateway executes 1 remote HTTP request dynamically.
