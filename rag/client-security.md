# Client Security (RAG-Friendly)

## Quick Answer

**How do I authenticate HTTP calls made via Middleware Client?**
Configure the `@MiddlewareContract` security parameter and define the credentials in the application YAML properties. There are four security modes (`NONE`, `PASSTHROUGH`, `API_KEY`, `OAUTH2_CLIENT_CREDENTIALS`).

**YAML Configuration:**
```yaml
client:
  product:
    security:
      type: API_KEY
      api-key: ${API_KEY_PRODUCT_SERVICE:default-secret-key}
```

**Java code (API\_KEY):**
```java
import io.github.spring.middleware.annotation.security.MiddlewareContract;
import io.github.spring.middleware.annotation.security.MiddlewareApiKey;

@MiddlewareContract(security = "API_KEY")
@MiddlewareApiKey(headerName = "X-API-KEY", value = "${client.product.security.api-key}")
public interface ProductClient {
    @GetMapping("/api/v1/products")
    List<ProductDto> listProducts();
}
```

**Constraints:**
- The security configuration type (`API_KEY`, `PASSTHROUGH`, etc.) in the properties must exactly match the `SecurityClientType` enum.
- Using `@MiddlewareApiKeyValue` on methods overrides the class-level default API Key.

---

## Token Forwarding (Passthrough)

### How do I forward an authorization token from a user request to a downstream service?
Use the `PASSTHROUGH` security type alongside the `@MiddlewarePassthrough` annotation.

**Java code:**
```java
@MiddlewareContract(security = "PASSTHROUGH")
@MiddlewarePassthrough(headerName = "Authorization", required = "true")
public interface CatalogClient {
    @GetMapping("/api/v1/catalog")
    List<CatalogDto> getCatalog();
}
```

**Configuration:**
```yaml
client:
  catalog:
    security:
      type: PASSTHROUGH
```

**Constraints:**
- The `SecurityPassthroughApplier` looks at the incoming request headers. If `required = "true"` and the header is missing in the current web context, it throws an exception immediately.
- The header name uses lowercase matching internally.

---

## OAuth2 Client Credentials

### How do I connect to an endpoint that requires an OAuth2 token?
Set the type to `OAUTH2_CLIENT_CREDENTIALS` and provide the Client ID, Secret, and Token URI in your configuration, then add the `@MiddlewareClientCredentials` annotation to the interface.

**YAML Configuration:**
```yaml
client:
  billing:
    security:
      type: OAUTH2_CLIENT_CREDENTIALS
      oauth2:
        client-id: ${OAUTH2_CLIENT_ID:billing-service}
        client-secret: ${OAUTH2_CLIENT_SECRET}
        token-uri: ${OAUTH2_TOKEN_URI:http://keycloak:8080/token}
```

**Java code:**
```java
@MiddlewareContract(security = "OAUTH2_CLIENT_CREDENTIALS")
@MiddlewareClientCredentials(
    tokenUri = "${client.billing.security.oauth2.token-uri}",
    clientId = "${client.billing.security.oauth2.client-id}",
    clientSecret = "${client.billing.security.oauth2.client-secret}"
)
public interface BillingClient {

    @GetMapping("/api/v1/invoices")
    @MiddlewareRequiredScopes({"billing.read"})
    List<InvoiceDto> getInvoices();
}
```

**Constraints:**
- Internally, `OAuth2ClientCredentialsClient` handles requesting and caching the token via Basic Auth against the endpoint.
- Tokens are automatically cached based on `tokenUri`, `clientId`, and the sorted method scopes until they expire.
- If the token endpoint fails, an `OAuth2TokenAcquisitionException` is deliberately thrown.
