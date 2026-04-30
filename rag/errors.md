# Error Model (RAG-Friendly)

## Quick Answer

**What is the Unified Error Model?**
Spring Middleware establishes a standard structured JSON error contract ensuring consistent domain error formats, HTTP statuses, and metadata forwarding (like request tracing) for both REST and GraphQL APIs.

**JSON Response Example:**
```json
{
  "statusCode": 404,
  "statusMessage": "Not Found",
  "code": "PRODUCT:NOT_FOUND",
  "message": "Product not found",
  "extensions": {
    "requestId": "F4D29AAFE7FC4844A1FF8794F186B102",
    "callChain": ["catalog-service", "product-service"]
  }
}
```

**Constraints:**
- The `code` field is always categorized in the `DOMAIN:KEY` format.
- A global `@RestControllerAdvice` translates business logic exceptions automatically formatting them as shown.

---

## Service Exceptions

### How do I emit a business logic error?
Always throw a `ServiceException` (or a subclass of it) that carries an `ErrorDescriptor`. Do NOT throw generic `RuntimeException` objects internally.

**Java context:**
The `ErrorDescriptor` dictates the generated HTTP status, the `DOMAIN:KEY` error Code, and standardizes descriptions. 

**Constraints:**
- The framework prevents leaking internal exceptions like `LazyInitializationException` to clients. Unmapped runtime errors fall back to `UNKNOWN_ERROR` giving HTTP `500 Internal Server Error`.
- `GraphQLValidationExceptionHandler` executes this same mapping exactly, placing the `code` inside the `extensions.code` of the GraphQL Error struct.

---

## Error Propagation

### What happens when an external microservice request fails?
The source framework packages the HTTP failure inside a `RemoteServerException` which wraps the JSON error thrown horizontally.

**Constraints:**
- The `extensions` metadata object is enriched sequentially as it propagates through boundaries, storing `remote.url`, `remote.method`, `requestId`, `spanId`, and the sequence `callChain` identifying exactly which hop failed.
