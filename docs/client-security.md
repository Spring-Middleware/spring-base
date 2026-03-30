# MiddlewareClient Security

This document describes the security options available for declarative middleware clients (`@MiddlewareClient`) and how to configure them.

Overview
--------
Middleware clients (the declarative `@MiddlewareClient` proxies) support several security modes so clients can authenticate to downstream services consistently.

Security modes (mapped to `SecurityClientType`)
- `NONE` — no security applied.
- `PASSTHROUGH` — a header from the incoming request is forwarded to the downstream service (useful to propagate caller auth tokens).
- `API_KEY` — the client attaches an API key header (value configured either at class level or per-method using annotations).
- `OAUTH2_CLIENT_CREDENTIALS` — the client obtains an OAuth2 access token using the client credentials flow and attaches `Authorization: Bearer <token>`.

Where to configure
------------------
Client-wide configuration lives under the `client` prefix. A typical YAML fragment (example):

```yaml
client:
  registry-endpoint: ${REGISTRY_ENDPOINT:http://localhost:8080/registry}
  product:
    security:
      type: OAUTH2_CLIENT_CREDENTIALS # Options: NONE, PASSTHROUGH, API_KEY, OAUTH2_CLIENT_CREDENTIALS
      api-key: ${API_KEY_PRODUCT_SERVICE:default-product-api-key}
      listProducts:
        api-key: ${API_KEY_PRODUCT_SERVICE:default-product-list-api-key}
      oauth2:
        client-id: ${OAUTH2_CLIENT_ID_PRODUCT_SERVICE:product-service}
        client-secret: ${OAUTH2_CLIENT_SECRET_PRODUCT_SERVICE}
        token-uri: ${OAUTH2_TOKEN_URI_PRODUCT_SERVICE:http://keycloak:8080/realms/spring-middleware/protocol/openid-connect/token}
```

Note: the effective security behavior is driven both by configuration and by annotations on the proxy interface/methods (see below).

Annotations and how they control per-client / per-method behavior
----------------------------------------------------------------
The declarative client APIs use annotations from `parent/api` to declare the security requirements of the proxy interface and its methods:

- `@MiddlewareClient` / `@MiddlewareContract` (class-level) — contains a `security` placeholder that resolves to one of the `SecurityClientType` names (e.g. `OAUTH2_CLIENT_CREDENTIALS`). The `ProxySecurityAnalyzer` resolves that value from the environment and decides which SecurityClientConfiguration to use.

- `@MiddlewarePassthrough(headerName = "Authorization", required = "true")` — indicates that the proxy expects passthrough behavior; configured header name is validated by the analyzer. When used and configured, `SecurityPassthroughApplier` will copy the header (if present) from the current request headers into the outbound WebClient call.

- `@MiddlewareApiKey(headerName = "X-API-KEY", value = "${client.api.key:}")` — class-level API key configuration. The analyzer reads the `value` (supports placeholders) and builds a `SecurityApiKeyClientConfiguration`. Per-method override is supported with `@MiddlewareApiKeyValue` on methods.

- `@MiddlewareClientCredentials(tokenUri = "${...}", clientId = "${...}", clientSecret = "${...}")` — class-level OAuth2 client credentials configuration. The analyzer validates required fields and builds a `SecurityClientCredentialsConfiguration` containing `tokenUri`, `clientId` and `clientSecret`.

- `@MiddlewareRequiredScopes(...)` (method-level) — used with client credentials mode to declare required token scopes for that method; these are passed to the token request and used in the token cache key.

Runtime flow (short)
--------------------
1. At startup or when creating proxy metadata, `ProxySecurityAnalyzer` inspects the proxy interface annotations and the `@MiddlewareContract.security()` placeholder resolved against the `Environment`. It returns a `SecurityClientConfiguration` instance for the client (none, passthrough, api-key, or client-credentials).

2. For each proxy method `MethodMetaDataExtractor` builds a `MethodSecurityConfiguration`:
   - If `OAUTH2_CLIENT_CREDENTIALS`, required scopes are collected (if any) into `ClientCredentialsMethodSecurityConfiguration`.
   - If `API_KEY`, the method or class-level API key value is resolved into `ApiKeyMethodSecurityConfiguration`.
   - Otherwise a `VoidMethodSecurityConfiguration` is used.

3. When a client invocation happens, `SecurityManagerApplier` selects a `SecurityApplier` implementation from `SecurityApplierFactory` based on the client `SecurityClientType` and calls `applySecurity(...)` with the client and method security configuration plus current headers and the `WebClient.RequestHeadersSpec<?>`.

4. The `SecurityApplier` implementations perform the concrete modifications:
   - `SecurityNoneApplier` — no changes.
   - `SecurityPassthroughApplier` — looks up the header (lower-cased) in `currentHeaders` and if present attaches it to the outbound request; throws if `required` and header missing.
   - `SecurityApiKeyApplier` — attaches the API key header taken from the `ApiKeyMethodSecurityConfiguration` (per-method value) or class-level config.
   - `SecurityClientCredentialsApplier` — obtains (and caches) an access token using `OAuth2ClientCredentialsClient` and attaches `Authorization: Bearer <token>`.

Token management
----------------
`OAuth2ClientCredentialsClient` handles token acquisition and caching. It:
- Builds a cache key from `tokenUri`, `clientId` and sorted scopes.
- Requests a token using `client_id`/`client_secret` via HTTP POST (form encoded) with Basic auth, and caches the access token until it expires.
- Throws a specific `OAuth2TokenAcquisitionException` on token endpoint failure so the proxy client can surface an actionable error.

Examples
--------
1) Passthrough (forward incoming Authorization header)

YAML (client-level security):

```yaml
client:
  product:
    security:
      type: PASSTHROUGH
```

Proxy interface annotations:

```java
@MiddlewareContract(security = "PASSTHROUGH")
@MiddlewarePassthrough(headerName = "Authorization", required = "true")
public interface ProductClient {
    @GetMapping("/api/v1/products")
    List<ProductDto> listProducts();
}
```

The `SecurityPassthroughApplier` will copy the incoming `Authorization` header into outbound requests.

2) API key per method

```java
@MiddlewareContract(security = "API_KEY")
@MiddlewareApiKey(headerName = "X-API-KEY", value = "${client.product.api-key:}")
public interface ProductClient {

    @GetMapping("/api/v1/products")
    @MiddlewareApiKeyValue("${client.product.list-api-key:}")
    List<ProductDto> listProducts();
}
```

The `SecurityApiKeyApplier` will add header `X-API-KEY: <value>` to the outbound request.

3) OAuth2 client credentials

Configuration (YAML):

```yaml
client:
  product:
    security:
      type: OAUTH2_CLIENT_CREDENTIALS
      oauth2:
        client-id: ${OAUTH2_CLIENT_ID_PRODUCT_SERVICE:product-service}
        client-secret: ${OAUTH2_CLIENT_SECRET_PRODUCT_SERVICE}
        token-uri: ${OAUTH2_TOKEN_URI_PRODUCT_SERVICE:http://keycloak:8080/realms/spring-middleware/protocol/openid-connect/token}
```

Proxy annotations:

```java
@MiddlewareContract(security = "OAUTH2_CLIENT_CREDENTIALS")
@MiddlewareClientCredentials(
  tokenUri = "${client.product.security.oauth2.token-uri}",
  clientId = "${client.product.security.oauth2.client-id}",
  clientSecret = "${client.product.security.oauth2.client-secret}"
)
public interface ProductClient {

    @GetMapping("/api/v1/products")
    @MiddlewareRequiredScopes({"products.read"})
    List<ProductDto> listProducts();
}
```

The `SecurityClientCredentialsApplier` will obtain an access token and add `Authorization: Bearer <token>`.

Notes and best practices
------------------------
- Prefer environment variables or secrets manager for sensitive values (client secrets, api keys).
- Use per-method annotations when you need to override class-level defaults (e.g. different API keys for different endpoints).
- For client credentials, prefer short-lived tokens and a robust token endpoint; the built-in client caches tokens until expiry.
- Validate headers and fail fast for missing required headers when using `PASSTHROUGH`.

Where to look in the codebase
----------------------------
- `parent/app/src/main/java/io/github/spring/middleware/client/proxy/security/ProxySecurityAnalyzer.java`
- `parent/app/src/main/java/io/github/spring/middleware/client/proxy/security/SecurityApplierFactory.java`
- `parent/app/src/main/java/io/github/spring/middleware/client/proxy/security/applier/*` (applier implementations)
- `parent/app/src/main/java/io/github/spring/middleware/client/proxy/security/oauth2/OAuth2ClientCredentialsClient.java`
- Annotations: `parent/api/src/main/java/io/github/spring/middleware/annotation/security/*`

---

## Related documentation

- [README.md](../README.md)
- [Getting Started](./getting-started.md)
- [Architecture](./architecture.md)
- [Communication](./communication.md)
- [Errors](./errors.md)
- [Registry](./registry.md)
- [Kafka](./kafka.md)
- [Logging](./logging.md)
- [Redis](./redis.md)
- [Mongo](./mongo.md)
- [JPA](./jpa.md)
- [RabbitMQ](./rabbitmq.md)
- [Security](./security.md)
- [Core](./core.md)
