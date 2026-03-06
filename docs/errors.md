# Error Model

This document describes the **unified error model** used across Spring Middleware for both HTTP and GraphQL APIs.

Spring Middleware ensures that errors are:

- structured and predictable
- enriched with contextual metadata
- consistently propagated across service boundaries

---

## 1. Goals

The error model is designed to:

- give clients a **stable error contract** (both for REST and GraphQL)
- allow services to attach **machine-readable error codes**
- preserve context such as **request ID**, **span ID**, and **call chain**
- distinguish **domain errors** from **technical failures**

---

## 2. Core types

Spring Middleware implements the error model using the following core classes and components:

- `ServiceException` – base exception for domain and application errors.
- `ErrorDescriptor` – structured description of an error (code, message, HTTP status, etc.).
- `ErrorMessage` – serializable representation of an error response.
- `ErrorMessageFactory` – factory for producing `ErrorMessage` instances from exceptions.
- `RemoteServerException` – wraps errors coming from remote services.
- Global `@RestControllerAdvice` – translates exceptions into HTTP responses using the unified model.
- `GraphQLValidationExceptionHandler` – centralized GraphQL exception handler.

These components work together to ensure that errors have a consistent JSON structure and that additional metadata is propagated correctly.

---

## 3. HTTP error representation

### 3.1 Standard payload

HTTP errors are returned with a JSON body similar to the following:

```json
{
  "statusCode": 404,
  "statusMessage": "Not Found",
  "code": "PRODUCT:NOT_FOUND",
  "message": "Product not found",
  "extensions": {}
}
```

Fields:

- `statusCode` – HTTP status code (e.g. `400`, `404`, `500`).
- `statusMessage` – symbolic name of the status (e.g. `Bad Request`, `Not Found`, `Internal Server Error`).
- `code` – **domain-specific error code**, typically `DOMAIN:KEY` (e.g. `PRODUCT:NOT_FOUND`).
- `message` – human-readable error message.
- `extensions` – map of additional metadata (see below).

### 3.2 Extensions and metadata

The `extensions` field can contain arbitrary key/value metadata to help debugging and correlation. Common entries include:

- `requestId` – value of the `X-Request-ID` header.
- `spanId` or `span` – span information for the current service.
- `callChain` – list describing the services and methods involved in the call.
- `remote.*` – details about remote calls (see below).

Example with metadata for a propagated error:

```json
{
  "statusCode": 404,
  "statusMessage": "Not Found",
  "code": "PRODUCT:NOT_FOUND",
  "message": "Product not found",
  "extensions": {
    "requestId": "F4D29AAFE7FC4844A1FF8794F186B102",
    "span": [
      {
        "service": "catalog",
        "method": "replaceProducts",
        "httpStatus": 404
      }
    ]
  }
}
```

This representation is used consistently for local errors and for errors propagated from remote services.

---

## 4. Error propagation across services

Spring Middleware integrates the error model with **service communication** (see `docs/communication.md`).

When a service calls another service via a `@MiddlewareClient` and the remote call fails:

- the remote service produces a structured `ErrorMessage` JSON response
- the caller parses that response
- a `RemoteServerException` (or similar) is created on the caller side, preserving metadata

Remote error metadata typically includes:

- `remote.url` – URL of the remote call
- `remote.method` – HTTP method used (GET, POST, etc.)
- `remote.service` – logical service/cluster name
- `requestId` – shared request identifier
- `spanId` – span identifier for the remote call
- `callChain` – sequence of calls across services

This information is available to application code (e.g., in exception handlers) and can be logged or translated as needed.

---

## 5. ServiceException and ErrorDescriptor

### 5.1 ServiceException

`ServiceException` is the base exception type for application and domain-level errors. Typical usage:

- thrown by service code when a known business rule fails
- carries an `ErrorDescriptor` (or equivalent fields) describing the error

Service authors should prefer throwing `ServiceException` (or subclasses) instead of raw `RuntimeException` for predictable behavior.

### 5.2 ErrorDescriptor

`ErrorDescriptor` is a structured description of an error. It usually contains:

- an **error code** (e.g. `PRODUCT:NOT_FOUND`)
- a **message template** or message key
- a **default HTTP status**
- optional **parameters** and **extensions**

The global exception mapping infrastructure uses the `ErrorDescriptor` to:

- determine the HTTP status      
- produce the final text message
- populate the `code` and `extensions` fields of the `ErrorMessage`

By centralizing error descriptions in `ErrorDescriptor`, services can:

- keep domain error codes consistent
- reuse error definitions across modules

---

## 6. ErrorMessage and ErrorMessageFactory

`ErrorMessage` is the **wire format** of an error – the exact shape serialized to JSON.

`ErrorMessageFactory` is responsible for constructing `ErrorMessage` instances from:

- `ServiceException`
- `ErrorDescriptor`
- generic or technical exceptions

The factory typically:

1. Resolves or creates an `ErrorDescriptor`.
2. Determines the appropriate HTTP status code and status message.
3. Builds the `ErrorMessage` with `statusCode`, `statusMessage`, `code`, `message`, and `extensions`.
4. Adds tracing metadata (request ID, span, call chain) if available.

Global exception handlers (such as `@RestControllerAdvice`) delegate to `ErrorMessageFactory` to ensure all responses follow the same structure.

---

## 7. GraphQL error representation

Spring Middleware applies the same error concepts to GraphQL.

GraphQL errors are returned according to the GraphQL specification, but the **domain error code** is exposed through the `extensions` field.

Example GraphQL error:

```json
{
  "message": "Product not found",
  "path": ["product"],
  "extensions": {
    "code": "PRODUCT:NOT_FOUND"
  }
}
```

Key points:

- `message` – human-readable message (client-facing).
- `path` – path to the field or operation where the error occurred.
- `extensions.code` – domain error code, consistent with the HTTP model.
- additional entries in `extensions` can be added as needed (e.g. `requestId`).

This makes GraphQL errors interoperable with the same error catalogs used for REST.

---

## 8. GraphQLValidationExceptionHandler

Spring Middleware provides a centralized GraphQL exception handler called `GraphQLValidationExceptionHandler`.

### 8.1 Purpose

- map known exception types to the unified error model
- convert them into GraphQL errors with a consistent shape
- enforce consistent error codes and messages across all GraphQL endpoints

### 8.2 Handled exceptions

According to the current design, `GraphQLValidationExceptionHandler` handles at least:

- `ServiceException` – domain and application errors
- `ErrorDescriptor` – direct descriptors
- `PersistenceException` – persistence-layer exceptions (e.g. Hibernate), with constraint-resolution logic
- `ConstraintViolationException` – Jakarta Bean Validation errors
- `LazyInitializationException` – typically ignored to avoid leaking internal details
- **fallback** – any other unhandled exception maps to a generic `UNKNOWN_ERROR` code (e.g. `FrameworkErrorCodes.UNKNOWN_ERROR`).

The handler translates these exceptions into GraphQL error responses by:

- resolving an appropriate error code
- mapping to a user-facing message
- optionally attaching validation details

This ensures that GraphQL clients see **stable error codes** even when underlying implementation details differ.

---

## 9. Global REST exception handling

For REST controllers, Spring Middleware uses a global `@RestControllerAdvice` that:

1. intercepts thrown exceptions
2. classifies them (domain vs technical vs unknown)
3. uses `ErrorMessageFactory` to build an `ErrorMessage`
4. returns a `ResponseEntity<ErrorMessage>` with the correct HTTP status

Typical mappings:

- `ServiceException` → domain error with specific code and status
- `RemoteServerException` → remote error, possibly rewrapping the remote `ErrorMessage`
- Validation / constraint exceptions → `400 Bad Request` with detailed messages
- Other runtime exceptions → `500 Internal Server Error` with generic `UNKNOWN_ERROR` code

This ensures that all REST endpoints in a service return errors with the same structure and metadata.

---

## 10. Guidelines for service authors

To take full advantage of the error model:

1. **Define clear error codes**  
   - Use a `DOMAIN:KEY` convention, e.g. `PRODUCT:NOT_FOUND`, `ORDER:INVALID_STATE`.
   - Group codes logically per domain or bounded context.

2. **Throw ServiceException (or equivalent)**  
   - Prefer throwing `ServiceException` or a domain-specific subclass when you detect a known error condition.
   - Attach an `ErrorDescriptor` with code, message key and HTTP status.

3. **Avoid leaking internal exceptions**  
   - Catch persistence or low-level technical exceptions and translate them into domain-specific errors where appropriate.
   - Let the global exception handlers deal with truly unexpected failures as `UNKNOWN_ERROR`.

4. **Preserve context**  
   - When you manually build or rethrow errors, keep `requestId`, `spanId` and `callChain` if available.
   - Use logging that includes these identifiers to ease debugging.

5. **Document error contracts**  
   - For public APIs, document important error codes and meanings.
   - Ensure React / web / mobile clients understand how to use `code` and `extensions`.

---

## 11. Relationship with other components

The error model is tightly integrated with other parts of Spring Middleware:

- **Service communication** – see `docs/communication.md` for how errors propagate across `@MiddlewareClient` calls.
- **Registry** – see `docs/registry.md` for how service metadata and topology can influence error handling (e.g. missing service or node).
- **GraphQL** – see `docs/graphql.md` for GraphQL-specific behavior and examples.

Together, these components provide a consistent, observable error story across the entire platform.
