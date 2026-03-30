# Security configuration

This document describes how HTTP security is configured in Spring Middleware.

Spring Middleware builds on top of Spring Security 6 (via Spring Boot 3.4.x) and exposes a small, opinionated configuration model through the `middleware.security.*` properties.

At a high level, the security layer provides:

- pluggable authentication modes (`SecurityType`)
- a common authorization model based on **public** and **protected** paths
- stateless HTTP security with CSRF disabled
- unified error responses for API key authentication failures

---

## 1. Overview

Security is configured per service using standard Spring Boot configuration properties:

```yaml
middleware:
  security:
    type: API_KEY   # or NONE, BASIC_AUTH, JWT, OIDC
    public-paths: []
    protected-paths: []
```

Key ideas:

- **Authentication mode** is selected with `middleware.security.type`.
- **Authorization rules** are defined via `public-paths` and `protected-paths`.
- Each security mode has its own configuration block (e.g. `basic`, `jwt`, `oauth2`, `api-key`).
- API key authentication failures are translated into the unified `ErrorMessage` JSON structure.

If no type is configured or `type: NONE` is used, all requests are permitted.

---

## 2. Supported security types (`SecurityType`)

The `middleware.security.type` property controls which security mode is active. Internally this maps to the `SecurityType` enum:

```java
public enum SecurityType {
    NONE,
    BASIC_AUTH,
    JWT,
    OIDC,
    API_KEY
}
```

### 2.1 NONE

- Property: `middleware.security.type=NONE` (or unset).
- Effect: security is **disabled**; all requests are permitted.
- Configuration: CSRF is disabled, `anyRequest().permitAll()`.
- Typical use cases: local development, internal testing, services running behind another trusted gateway.

### 2.2 BASIC_AUTH

- Property: `middleware.security.type=BASIC_AUTH`.
- Effect: enables HTTP Basic authentication using Spring Security.
- User details can come from:
  - configuration (`middleware.security.basic.credentials`), or
  - a custom `UserApi` implementation.
- Authorization uses the common **protected path rules**.

### 2.3 JWT

- Property: `middleware.security.type=JWT`.
- Effect: configures the service as a **JWT resource server**.
- JWT validation is driven by `middleware.security.jwt.*` properties.
- Authorities / roles are extracted from a claim (e.g. `roles`).
- Authorization uses the common **protected path rules**.

### 2.4 OIDC

- Property: `middleware.security.type=OIDC`.
- Effect: configures the service as an **OIDC-aware resource server**.
- Token validation is driven by `middleware.security.oauth2.*` properties (issuer / JWKs).
- Authorities / roles are extracted from a configured claim path.
- Authorization uses the common **protected path rules**.

### 2.5 API_KEY

- Property: `middleware.security.type=API_KEY`.
- Effect: enables **API key authentication** using a header (default `X-Api-Key`).
- API keys and their roles are defined under `middleware.security.api-key.*`.
- Requests to protected paths without a valid API key result in unified error responses.

---

## 3. Common authorization model

All security modes share the same **authorization** model, based on two concepts:

- `publicPaths` – paths that are always allowed without authentication.
- `protectedPaths` – paths that require authentication and specific roles.

These are configured using `middleware.security.public-paths` and
`middleware.security.protected-paths`.

### 3.1 Public paths

Public paths are patterns that are always accessible, regardless of the current user or API key.

```yaml
middleware:
  security:
    type: API_KEY
    public-paths:
      - /actuator/health
      - /api-docs/**
      - /swagger-ui.html
      - /swagger-ui/**
```

Notes:

- Patterns are Ant-style (e.g. `/api/**`).
- Public paths are useful for health checks, documentation UIs, or public resources.

### 3.2 Protected paths

Protected paths define which requests require authentication and what roles are required.
They are configured as a list of rules matching the `SecurityConfigProperties.ProtectedPathRule` structure.

```yaml
middleware:
  security:
    type: API_KEY
    protected-paths:
      - type: ROLES
        path: /api/*/catalogs/*/products
        methods: [ GET ]
        allowed-roles: [ LIST_PRODUCTS, ADMIN ]
      - type: ROLES
        path: /api/*/catalogs/**
        methods: [ GET ]
        allowed-roles: [ GET_CATALOG, ADMIN ]
```

Each rule supports:

- `type` – strategy of the rule. Controls how the path is protected:
  - `NONE` – the rule marks the path as **public** (no authentication required).
  - `AUTHENTICATED` – the path requires the user to be **authenticated**, but no specific role is enforced.
  - `ROLES` – the path requires the user to be **authenticated** and to have at least one of the roles in `allowed-roles`.
- `path` – Ant-style path pattern.
- `methods` – list of HTTP methods (e.g. `GET`, `POST`). Empty means "all methods".
- `allowed-roles` – list of logical roles authorized for this rule. Only used when `type = ROLES`.

At runtime:

- A request is matched against **public paths** first (permit all).
- Then it is evaluated against all protected rules according to their `type`:
  - If a rule with `type = NONE` matches, the request is treated as public (`permitAll`).
  - If a rule with `type = AUTHENTICATED` matches, the request requires authentication (`authenticated()`), but no role check is done.
  - If a rule with `type = ROLES` matches, the user must be authenticated and have at least one of the configured roles.
- Roles are mapped to Spring Security authorities with the `ROLE_` prefix (e.g. `ADMIN` -> `ROLE_ADMIN`).

Any request that is not public and does not match a protected rule falls back to `authenticated()` semantics in the configured mode.

---

## 4. Configuration properties

All properties live under the `middleware.security` prefix and are provided by `SecurityConfigProperties`.

### 4.1 Top-level properties

```yaml
middleware:
  security:
    type: NONE | BASIC_AUTH | JWT | OIDC | API_KEY
    public-paths: []          # List<String>
    protected-paths: []       # List<ProtectedPathRule>
```

### 4.2 Basic authentication

```yaml
middleware:
  security:
    type: BASIC_AUTH
    basic:
      credentials:
        username: admin
        password: changeme
        roles: [ ADMIN ]
      user-api:
        enabled: false
```

- `basic.credentials` – single in-memory user definition.
- `basic.credentials.roles` – logical roles for that user.
- `basic.user-api.enabled` – when `true`, the framework expects a `io.github.spring.middleware.security.basic.UserApi`
  bean and delegates user loading to it instead of using in-memory credentials.

You can still combine this with `public-paths` and `protected-paths` to define which endpoints require which roles.

### 4.3 JWT

```yaml
middleware:
  security:
    type: JWT
    jwt:
      secret: ${JWT_SECRET:change-me}
      algorithm: HS256
      authority-claim-name: roles
```

- `jwt.secret` – HMAC secret used to validate JWTs.
- `jwt.algorithm` – algorithm enum (defaults to `HS256`).
- `jwt.authority-claim-name` – claim from which roles/authorities are extracted (default `roles`).

JWT validation is configured by the `JwtSecurityConfigurer` and related beans. The token is usually passed in the
`Authorization: Bearer <token>` header.

### 4.4 OIDC

```yaml
middleware:
  security:
    type: OIDC
    oauth2:
      issuer-uri: https://issuer.example.com/realms/demo
      jwk-set-uri: https://issuer.example.com/realms/demo/protocol/openid-connect/certs
      authorities-claim-path: $.realm_access.roles
```

- `oauth2.issuer-uri` – OIDC issuer URI.
- `oauth2.jwk-set-uri` – JWK Set URI to resolve signing keys.
- `oauth2.authorities-claim-path` – JSON path used to extract role names from the token.

The OIDC mode also behaves as a stateless resource server and reuses the common authorization model.

### 4.5 API key

```yaml
middleware:
  security:
    type: API_KEY
    public-paths:
      - /api-docs/**
      - /swagger-ui.html
      - /swagger-ui/**
    protected-paths:
      - type: ROLES
        path: /api/*/catalogs/*/products
        methods: [ GET ]
        allowed-roles: [ LIST_PRODUCTS, ADMIN ]
      - type: ROLES
        path: /api/*/catalogs/**
        methods: [ GET ]
        allowed-roles: [ GET_CATALOG, ADMIN ]
    api-key:
      header-name: X-API-KEY
      credentials:
        - key: ${API_KEY_CATALOG_SERVICE:default-api-key}
          enabled: true
          roles: [ GET_CATALOG, LIST_PRODUCTS ]
```

- `api-key.header-name` – HTTP header to read the API key from (default: `X-Api-Key`).
- `api-key.credentials` – list of configured API keys and their roles.
  - `key` – API key value (typically injected via environment variable).
  - `enabled` – whether the key is active.
  - `roles` – logical roles that this key grants.

You can define multiple credentials entries for different consumers (e.g. internal services, external partners) with
separate roles.

---

## 5. API key authentication flow

The API key mode is implemented by `ApiKeyAuthenticationFilter` and related components.

High-level flow:

1. The filter reads the header defined by `middleware.security.api-key.header-name` (default `X-Api-Key`).
2. It uses `ProtectedPathRuleResolver` to determine if the current request matches a protected path.
3. If the request is protected and **no** API key is present, a `401` response is returned using the
   `MiddlewareAuthenticationEntryPoint`.
4. If an API key is present, it is resolved via `ApiKeyRetriever` to an `ApiKeyDetails` instance.
5. If the key is missing, disabled, or unknown, the entry point returns a `401` with a structured error message.
6. If the key is valid, an `Authentication` is created with authorities derived from `ApiKeyDetails.roles` and stored
   in the `SecurityContext`.
7. The request continues through the filter chain.

Because the authorization decision is driven by `protected-paths`, you can mix public and protected endpoints in the
same service while using a single API key configuration.

---

## 6. Error handling

Security failures behave differently depending on the active security type.

- **API_KEY**  
  Authentication failures (for example, missing or invalid API key) are handled by
  `MiddlewareAuthenticationEntryPoint`, which uses the shared `ErrorMessage` model.
  401 responses produced by the API key filter follow the same structured JSON shape
  described in `docs/errors.md`.

- **BASIC_AUTH / JWT / OIDC**  
  For these modes, authentication and authorization errors (401/403) are handled by the standard
  Spring Security entry points and access denied handlers. They do **not** automatically use the
  `ErrorMessage` JSON structure.

For non-security exceptions thrown inside your REST controllers, the global
`CommonsControllerAdvice` still applies and converts them into `ErrorMessage` responses, regardless
of the active security type.

---

## 7. Extensibility

The security layer is designed to be extensible:

- A `SecurityConfigurer` bean can be provided for each `SecurityType`. Custom types can be added by implementing this
  SPI and wiring a new `SecurityConfigurer`.
- For API keys, you can provide a custom `ApiKeyRetriever` bean to load keys from a database, external service, or
  another source instead of configuration.
- For JWT / OIDC, you can customize how authorities are resolved by providing a custom `Converter<Jwt, ? extends
  AbstractAuthenticationToken>` or by adjusting configuration (e.g. claim names / JSON paths).

Method-level security annotations (e.g. `@RolesAllowed`) are also enabled via `@EnableMethodSecurity(jsr250Enabled =
true)` in `MiddlewareSecurityConfiguration`, so you can combine URL-based rules with method-level checks when needed.

---

## Related documentation

- [README.md](../README.md) — high-level project overview
- [Architecture](./architecture.md)
- [Communication](./communication.md)
- [Errors](./errors.md)
- [Registry](./registry.md)
- [Client Security](./client-security.md)
- [Logging](./logging.md)
- [Kafka](./kafka.md)
- [Redis](./redis.md)
- [Mongo](./mongo.md)
- [JPA](./jpa.md)
- [RabbitMQ](./rabbitmq.md)
- [Core](./core.md)
