# Core Module (RAG-Friendly)

## Quick Answer

**What is the core of Spring Middleware?**
The core functionality is split into four essential modules that provide the fundamental APIs, shared libraries, and application-level utilities. 
- `api`: Interfaces and annotations (`@MiddlewareContract`, `@MiddlewareCircuitBreaker`, etc.)
- `app`: Runtime wiring, filters (`RequestLoggingFilter`), resilience configuration, and AutoConfigurations.
- `commons`: Converters, DTOs, validations constraints, and the Unified Error Model.
- `view`: Response-shaping and view authorization helpers.

**Constraints:**
- The `api` module must remain annotation-focused and stable. Any runtime logic/beans should go into `app`.
- The `commons` module prevents duplication by sharing low-level utilities natively across all other middleware modules.

---

## Middleware Client Resilience defaults

### How are default connections defined for Declarative Clients?
You override or specify default resilience and security configurations per client Name through the `app` module using application properties.

**YAML Configuration:**
```yaml
middleware:
  client:
    defaultMaxConcurrent: 25 # global internal bulkhead semaphore fallback
    product: # The Client name
      connection:
        timeout: 30000
        max-connections: 50
        max-retries: 3
```

**Constraints:**
- These properties map directly to `@MiddlewareContractConnection` and `@MiddlewareCircuitBreaker` annotation placeholders used in the `api` module proxy interfaces.

---

## View Module

### How do I shape response JSON fields based on Roles?
Use the `view` module's `PropertyRolesAllowedAuthorizer` and `DataAdaptor` ecosystem.

**Concept:**
It provides a `View` interface allowing the controller to transform internal domain objects dynamically. If a user holds the `ADMIN` role, they might see the `salary` field, whereas a `USER` role response shapes the JSON strictly without it, keeping the REST Controller thin.
