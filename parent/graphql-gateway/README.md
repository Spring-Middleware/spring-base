[![Maven Central](https://img.shields.io/maven-central/v/io.github.spring-middleware/bom.svg)](https://search.maven.org/artifact/io.github.spring-middleware/bom)

# GraphQL Gateway Module

Gateway that composes service GraphQL schemas and exposes a unified API. For overarching documentation see `../../README.md`.

See module docs: [docs/graphql.md](../../docs/graphql.md)

This module contains Docker helper scripts and a runtime Dockerfile under `boot/`.

---

# GraphQL Gateway

This module provides a **central GraphQL gateway** for Spring Middleware.

The gateway:

- discovers GraphQL schemas registered in the **Registry Service**
- builds an executable `GraphQL` engine from those schemas
- exposes a single `/graphql` HTTP endpoint for clients
- applies shared scalar types and runtime utilities from the `service` module

## Modules

The `graphql-gateway` parent is split into:

- `boot` – Spring Boot application packaging and runtime configuration
- `service` – core GraphQL gateway runtime (schema loading, composition, execution, scalars)
- `event` – internal events around schema locations and gateway lifecycle

Most users only interact with the **`boot` application** and treat the other modules as internal implementation.

## HTTP API

The gateway exposes a single HTTP endpoint:

- `POST /graphql`

The request body follows the standard GraphQL over HTTP JSON shape:

```json
{
  "query": "query User($id: ID!) { user(id: $id) { id name } }",
  "operationName": "User",
  "variables": {
    "id": "123"
  }
}
```

The response uses the standard GraphQL specification shape:

```json
{
  "data": { ... },
  "errors": [ ... ]
}
```

## Runtime Overview

At runtime the gateway:

1. Connects to the **Registry Service** using the standard middleware client configuration.
2. Discovers registered GraphQL schema locations for the platform.
3. Loads each schema and builds a `TypeDefinitionRegistry` per schema location.
4. Composes the registries into a single executable `GraphQL` schema.
5. Executes incoming GraphQL operations via `/graphql`.

Internally, this behavior is implemented by classes under:

- `io.github.spring.middleware.graphql.gateway.runtime`
- `io.github.spring.middleware.graphql.gateway.util`
- `io.github.spring.middleware.graphql.gateway.scalars`

These classes are meant for internal use and may evolve without being considered a public API.

## Configuration

The `boot` module provides the main configuration via `application.yml`:

```yaml
server:
  port: ${SERVER_PORT:8080}
  servlet:
    context-path: ${SERVER_CONTEXT_PATH:/}

middleware:
  client:
    registryEndpoint: ${REGISTRY_ENDPOINT:http://localhost:8080/registry}

  resourceRegister:
    clusterName: ${RESOURCE_CLUSTER_NAME:graphql-gateway}

  publicServer:
    host: ${PUBLIC_SERVER_HOST:localhost}
    port: ${PUBLIC_SERVER_PORT:8070}

  graphql:
    enabled: ${GRAPHQL_ENABLED:false}

  registryConsistencyScheduler:
    enabled: ${REGISTRY_CONSISTENCY_SCHEDULER_ENABLED:false}
```

### Key properties

- `REGISTRY_ENDPOINT` – URL of the Registry Service used to discover GraphQL schema locations.
- `RESOURCE_CLUSTER_NAME` – logical cluster name under which the gateway registers itself.
- `PUBLIC_SERVER_HOST` / `PUBLIC_SERVER_PORT` – advertised public endpoint of the gateway.
- `GRAPHQL_ENABLED` – enables internal GraphQL features from Spring Middleware (used by services; usually `false` for the gateway itself).
- `REGISTRY_CONSISTENCY_SCHEDULER_ENABLED` – registry self-healing for the gateway node.

## Running locally

To run the GraphQL gateway locally:

1. Ensure the **Registry Service** is running and accessible.
2. Build the project from the repository root:

   ```bash
   mvn clean package -DskipTests
   ```

3. Start the gateway from the `boot` module:

   ```bash
   cd parent/graphql-gateway/boot
   mvn spring-boot:run
   ```

4. Send a GraphQL request to:

   - `http://localhost:8080/graphql`

Example with `curl`:

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ __schema { queryType { name } } }"
  }'
```

## Docker image

The repository includes a helper script and Dockerfiles to build a Docker image for the gateway.

From the repository root:

```bash
./tools/build-graphql-gateway-image.bat
```

This uses the `Dockerfile.runtime` in `parent/graphql-gateway/boot` and the built JAR from `parent/graphql-gateway/boot/target`.

The resulting image is tagged as:

- `spring-base_graphql-gateway:latest`

You can then run it with:

```bash
docker run --rm -p 8080:8080 spring-base_graphql-gateway:latest
```

Make sure to pass the appropriate environment variables for the Registry endpoint and logging as needed.

## Relationship with Registry

The GraphQL gateway is a **consumer of Registry data**:

- services using Spring Middleware register their GraphQL schema locations in the Registry
- the gateway reads these `SchemaLocation` entries and builds the unified GraphQL API

For details about schema registration, see `docs/registry.md` and `docs/graphql.md`.

## Security

The current gateway focuses on **infrastructure integration and schema composition**.

Authentication and authorization can be implemented in front of the gateway (e.g. API gateway, OAuth2 proxy) or inside this service using the standard Spring Security patterns used elsewhere in the platform.

Future iterations of the platform may provide a dedicated security configuration for the GraphQL gateway similar to other modules.
