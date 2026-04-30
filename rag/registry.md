# Registry (RAG-Friendly)

## Quick Answer

**What is the Registry Service?**
It is the centralized control plane that maintains tracking data for Cluster topology, Node endpoints, Node health, REST capabilities (`@Register`), and GraphQL (`SchemaLocation`) topology.

**How do I configure my service to talk to the Registry?**
Define the connection URL under the `spring.middleware.registry` property.

**YAML Configuration:**
```yaml
spring:
  middleware:
    registry:
      url: ${REGISTRY_ENDPOINT:http://registry.local:8080/registry}
      enabled: true
```

**Constraints:**
- Microservices automatically perform consistency checks with the registry. If a node loses its registration (or the registry reboots), it automatically re-registers its endpoints seamlessly.
- The GraphQL Gateway does NOT execute GraphQL models directly from the database; it dynamically queries the Registry for `SchemaLocation` entries periodically.

---

## Clusters and Nodes

### What happens when my Spring Boot app starts up?
1. The framework scans instances annotated with `@Register` (Controllers).
2. It lists all GraphQL Schema metadata.
3. It packages these and registers them dynamically to the remote Registry URL under its own logical `Cluster` alias.
4. The Registry defines `RegistryEntry` structures with `nodeEndpoints` containing precise routing paths.

**Constraints:**
- Your downstream `@MiddlewareClient(service="products")` uses this central `nodeEndpoints` map. If a node fails a heartbeat, the registry removes it, and communication stops attempting those bad routes automatically.
