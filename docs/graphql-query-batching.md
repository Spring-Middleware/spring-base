# Declarative GraphQL Batching and Links

This document explains how Spring Middleware implements links between GraphQL schemas using the `@GraphQLLink` annotation family. Through these tools, the GraphQL API Gateway composes queries and resolves linked fields distributed across different microservices.

One of the most advanced and unique features of Spring Middleware is **Declarative GraphQL Batching**. This innovative approach allows grouping multiple queries (N+1 problem) to microservices without needing to implement complex resolvers in code, purely through configuration and annotations.

---

## Declarative Batching (NEW and TOP!)

The N+1 problem commonly occurs in GraphQL when resolving a list of entities (e.g., several `Catalog` items) and each needs to fetch additional data from another entity or microservice (e.g., `Product`).

Spring Middleware introduces a declarative and transparent solution to batch these subqueries from multiple requests into a single automatic call.

### Configuration

To use this feature, you first need to enable the gateway feature in the configuration, for example from `application.yml` or via environment variables in the `graphql-gateway` microservice:

```yaml
graphql:
  gateway:
    batching:
      enabled: ${GRAPHQL_GATEWAY_BATCHING_ENABLED:true}
```

### Batching Annotations

To declare a batched link relationship, use the following parameters in the annotations:

- `@GraphQLLink(..., batched = true)` – Informs the Gateway that list resolutions for this field should be batched.
- `@GraphQLLinkArgument(..., batch = true, targetFieldName = "id")` – Specifies which remote argument will receive the list of batched values, and on which field (`targetFieldName`) of the remote response the in-memory match should be performed to reassign the results to the original parent entities.

### Full Example: Catalog and Product

In the following distributed example, the `Catalog` entity links to `Products`, delegating the query to the catalog service:

**Catalog Service (Domain)**:

```java
package io.github.spring.middleware.catalog.domain;

import io.github.spring.middleware.annotation.graphql.GraphQLLink;
import io.github.spring.middleware.annotation.graphql.GraphQLLinkArgument;
import io.github.spring.middleware.annotation.graphql.GraphQLLinkClass;
import io.github.spring.middleware.annotation.graphql.GraphQLType;
import io.github.spring.middleware.graphql.arguments.GraphQLLinkArguments;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@GraphQLLinkClass(types = {@GraphQLType(names = "Catalog"), @GraphQLType(names = "Page_Catalog", isWrapper = true)})
public class Catalog {

    private UUID id;
    private String name;
    private String description;
    private CatalogStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    // MAGIC: Resolution with Declarative Batching
    @GraphQLLink(
        schema = "product", 
        type = "Product", 
        query = "productsByIds", 
        collection = true, 
        batched = true,
        arguments = {
            @GraphQLLinkArgument(name = "ids", targetFieldName = "id", batch = true)
        }
    )
    private List<UUID> productIds;

    @GraphQLQuery(name = "products")
    public List<UUID> getProductIds() {
        return productIds;
    }

    // Multi-variable Link (without batching)
    @GraphQLLink(
        schema = "product", 
        type = "Page_Product", 
        query = "products", 
        arguments = {
            @GraphQLLinkArgument(name = "catalogId"), 
            @GraphQLLinkArgument(name = "q")
        }
    )
    @GraphQLQuery(name = "productsByNames")
    public GraphQLLinkArguments getProductNames(@GraphQLArgument(name = "name") String name) {
        return new GraphQLLinkArguments(Map.of("catalogId", id, "q", name));
    }
}
```

**Product Service (GraphQL Controller)**:

This component must provide the target method `productsByIds` that will receive the flat batched list and return the associated objects:

```java
package io.github.spring.middleware.product.controller;

import io.github.spring.middleware.graphql.annotations.GraphQLService;
import io.github.spring.middleware.product.domain.Product;
import io.github.spring.middleware.product.domain.ProductStatus;
import io.github.spring.middleware.product.dto.graphql.DigitalProductInput;
import io.github.spring.middleware.product.dto.graphql.PhysicalProductInput;
import io.github.spring.middleware.product.dto.graphql.ProductInput;
import io.github.spring.middleware.product.mapper.ProductMapper;
import io.github.spring.middleware.product.service.ProductService;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@GraphQLService
public class ProductGraphqlController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductGraphqlController(ProductService productService, ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    // Exposes the list used to resolve the relationship to catalogs in a batched way:
    @GraphQLQuery(name = "productsByIds")
    public List<Product> getProductsByIds(@GraphQLArgument(name = "ids") List<UUID> ids) {
        return productService.getProductsByIds(ids);
    }

    @GraphQLQuery(name = "product")
    public Product getProduct(@GraphQLArgument(name = "id") UUID id) {
        return productService.getProduct(id);
    }

    @GraphQLQuery(name = "products")
    public Page<Product> listProducts(
            @GraphQLArgument(name = "q") String q,
            @GraphQLArgument(name = "status") ProductStatus status,
            @GraphQLArgument(name = "catalogId") UUID catalogId,
            @GraphQLArgument(name = "page") Integer page,
            @GraphQLArgument(name = "size") Integer size,
            @GraphQLArgument(name = "sort") String sort) {

        Pageable pageable = PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 20,
                sort != null ? Sort.by(sort.split(",")) : Sort.unsorted()
        );
        return productService.listProducts(q, status, catalogId, pageable);
    }
    
    // ...rest of mutations...
}
```

---

## Basic GraphQL Links (Non-Batched)

Besides advanced declarative batching, the `@GraphQLLink` family supports the following basic styles:

- **Simple argument mapping (Single argument)**: the underlying field value is passed directly as an argument to the remote service.
- **Multi-argument mapping**: the method returns a Map object or the `GraphQLLinkArguments` helper defining the different target names and their respective values.

### Collection architecture in microservices

During startup, each microservice:
1. Uses `GraphQLLinkedTypeBuilder` to map models marked with `@GraphQLLinkClass` / `@GraphQLType`.
2. Generates definitions (`GraphQLFieldLinkDefinition`) with the remote schema name, target queries, and corresponding IDs.

### How it works behind the Gateway

On the API Gateway side (`graphql-gateway`):
1. Intercepts invocations for "Linked" fields.
2. Delegates to `RemoteDelegatingGraphQLLinkDataFetcher`.
3. For the Batching option, resolvers identify pending fields at the ExecutionTree level (DataLoader / DataFetcher) based on identifiers, and build the batched GraphQL request requesting everything at once via `InLineFragmentBuilder` and `SelectionSetBuilder`.
4. Once the response returns with the flat array of remote objects, a matching system extracts the `targetFieldName` of each object and associates them accordingly to the original required entity, transparently solving the N+1 problem across the entire distributed network.

---

## Tips for microservice developers

- Apply the `@GraphQLLink` tag on the **field** when mapping the relationship directly against its IDs.
- Apply `@GraphQLLink` on the **function (getter)** if returning a map and it's a multi-argument link (Multi-variable).
- Must always be accompanied by `@GraphQLQuery(name = "xxx")`.
- For polymorphic responses (e.g., returning Interface subtypes), the framework manages and preserves them correctly within the execution tree.

### Related Links
- [GraphQL Overview](./graphql.md)
