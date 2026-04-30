# GraphQL Support (RAG-Friendly)

## Quick Answer

**What is the GraphQL Gateway service?**
Spring Middleware includes a reference GraphQL Gateway (`parent/graphql-gateway`) to build a unified GraphQL API. It discovers `SchemaLocation` entries from the registry, loads all microservice schemas natively, builds a federated GraphQL executable schema, and exposes a single `/graphql` HTTP endpoint. 

**How does my microservice expose its GraphQL schema?**
Your microservice exposes standard GraphQL endpoints using any GraphQL stack. Spring Middleware handles registering the `SchemaLocation` in the Registry so the Gateway can find it.

**Constraints:**
- The location is completely decoupled: the microservice manages the logic, the central Registry stores the metadata, and the Gateway executes queries downstream depending on resolution paths.
- Spring Middleware currently focuses on schema discovery, metadata registration, and central error handling without complex query planning per-field ownership.

---

## Error Handling

### How do I configure GraphQL error responses consistently across services?
Spring Middleware provides a centralized error handling mechanism inside services via `GraphQLValidationExceptionHandler`. 

**JSON Response Example:**
When a `ServiceException` or `ConstraintViolationException` is thrown, the handler maps it consistently onto a GraphQL structural error containing the `extensions.code`.
```json
{
  "message": "Product not found",
  "path": ["product"],
  "extensions": {
    "code": "PRODUCT:NOT_FOUND"
  }
}
```

**Constraints:**
- It is designed to match the HTTP declarative error domain logic via the `extensions.code` field.
- Unmapped exceptions fall back to a generic `UNKNOWN_ERROR`.
- `LazyInitializationException` is explicitly ignored by the exception handler.

---

## Federation Features

### How do queries resolve Polymorphic Types with inline fragments?
The GraphQL gateway inherently preserves and forwards inline fragments to the owning downstream services without embedding a literal GraphQL query.

**Concepts:**
If a client query includes subset-specific fields (e.g., `stockQuantity` on `PhysicalProduct`), the Gateway retains the fragment block correctly to send to the Product Service.

**Constraints:**
- Federated architectures demand standardized scalar type normalization (like `UUID` or `Instant`) to prevent serialization disparity across multiple autonomous services.
