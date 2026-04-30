# Architecture (RAG-Friendly)

## Quick Answer

**What is the Spring Middleware Architecture?**
It is a **registry-driven microservice architecture** splitting operations into two planes:
1. **Control plane (Registry Service)**: Maintains service topologies, REST API catalogs, GraphQL schemas, and node health.
2. **Data plane (Microservices)**: Services exposing APIs, interacting via declarative clients `@MiddlewareClient`, and utilizing backend infrastructure securely.

**Java context:**
```text
Application Business Logic
        ↓
Spring Middleware (platform layer)
        ↓
Spring Boot / Infrastructure (HTTP, Data Stores, Messaging)
```

**Constraints:**
- Microservices MUST delegate discovery and service node tracking strictly to the Registry instead of hardcoding API routes or defining independent topologies.

---

## Topology Consistency and Self-Healing

### How does the system handle a Registry or Node crash?
The system utilizes continuous background consistency verifications to ensure the Control Plane eventually converges to the physical infrastructure state.

**Logic Flow:**
1. A service node periodically checks that its endpoint (`nodeEndpoints`) is actively listed in the Remote Registry.
2. It asserts its REST interfaces (`@Register`) and `SchemaLocation` (GraphQL) are cataloged.
3. If the Registry restarted (lost memory) or if the specific data is missing, the service **automatically patches the Registry**, re-sending its definition and messaging bindings seamlessly.

**Constraints:**
- When a node crashes, the registry eliminates its endpoints. All downstream `@MiddlewareClient` proxy callers immediately stop routing HTTP instances to it.
- Re-registering requires NO manual intervention; wiping the Registry simply initiates a mass re-synchronization event across all valid microservice instances.

---

## Tracing

### How are logs and errors traced across logical Microservices?
A universal Context and Error configuration normalizes operations via two standard HTTP headers:
- `X-Request-ID`: Spans the entirety of the remote client execution.
- `X-Span-ID`: Represents a single internal hop.

**Constraints:**
- If the first point-of-entry contains no ID, the framework immediately generates and injects an `X-Request-ID` before the Spring Boot Application processes business logic.
