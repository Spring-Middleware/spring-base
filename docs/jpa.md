# JPA Module

Overview
--------
The JPA module provides a small, flexible framework to build parameterized JPQL queries from typed "search" DTOs. It is designed to keep repository code declarative: you define a Search DTO annotated with the provided annotations and the module composes the JPQL query at runtime.

This module is focused on the following goals:
- Build safe, parameterized JPQL from POJO search objects.
- Support joins, nested (sub) searches, concatenation, date handling and inclusion operators.
- Keep query generation testable and extensible via small builder components.

Key capabilities
----------------
- Declarative search using annotated Search DTOs (@SearchProperty, @SearchProperties, @SubSearch, @SearchForClass, ...)
- Automatic JOIN handling when a search property references a related path
- Concatenation support (UPPER(CONCAT(...))) for multi-field string search
- Inclusion operators (IN / NOT_IN) for collection-valued properties
- Compare operators for non-equality comparisons (>, <, >=, <=)
- Pre-conditions: inject raw JPQL conditions before a generated condition (AND/OR)
- Nested sub-searches: build a WHERE fragment from a nested search object
- Extensible condition builders via @ConditionBufferBuilderClass and a Spring-based factory
- Pagination, ordering and optional post-filtering support in the repository base implementation

Primary classes
----------------
- `SearchRepositoryImpl` — repository base implementation: builds a JPQL query using a `QueryBuffer`, parameterizes it (`QueryParameterizer`) and executes it with `EntityManager`.
- `QueryBuffer` + `QueryBufferParameters` — orchestrate building the JPQL string (SELECT, FROM, JOIN, WHERE, ORDER BY).
- `JoinBuffer` — collects/normalizes join clauses and prevents duplicate joins.
- `WhereBuffer`, `ConditionBuffer` — build WHERE clauses using small condition builders.
- `QueryParameterizer`, `ParameterCounter` — create parameter names and bind values to the JPA `Query`.
- `buffer.builder` implementations — concrete builders (SearchProperty, SearchProperties, SubSearch, Exists) that transform annotations + field values into condition fragments.

Important annotations (summary)
-------------------------------
- `@SearchForClass(value = X.class, distinct = true|false)`
  Indicates the entity class to build the FROM/SELECT for and whether SELECT DISTINCT should be used.

- `@SearchProperty(value = "prop.path", isLike = true|false, compareOperator = ..., inclusionOperator = ..., join = @Join(...), concat = @Concat(...), searchForNull = true|false, preCondition = @PreCondition(...))`
  The main annotation used on search DTO fields. Options:
  - `isLike` — build a LIKE-based condition (UPPER(...) LIKE :param)
  - `compareOperator` — for >, <, etc. when not using LIKE
  - `inclusionOperator` — IN/NOT_IN when the property is a collection
  - `join` — supply join metadata (value, left/right/fetch). The engine appends the join clause and reuses joins when the same join value appears.
  - `concat` — build UPPER(CONCAT(...)) across multiple properties
  - `searchForNull` — if true, a null search value can produce an IS NULL predicate
  - `preCondition` — raw JPQL fragment that will be prepended before the generated condition (useful for status checks)

- `@SearchProperties({...})` — define multiple `@SearchProperty` entries on a single field; the generated fragment ORs the individual properties.
- `@SubSearch` — a field whose value is itself a `Search` object; the framework embeds the sub-search WHERE fragment.
- `@SearchPropertyExists(value = "collectionProp")` — generates `is not empty` / `is empty` checks when a Boolean is provided.
- `@Join` — used either directly on `@SearchProperty.join` or as a meta-annotation on other annotations.

How it works (high level)
-------------------------
1. `SearchRepositoryImpl.createQueryForSearch(...)` creates a `QueryBufferParameters` instance and asks `QueryBuffer` to build the JPQL string.
2. `QueryBuffer` builds SELECT and FROM (reads `@SearchForClass`), asks `WhereBuffer` to build WHERE using condition builders and `JoinBuffer` for joins.
3. Builders (e.g. `SearchPropertyConditionBufferBuilderImpl`) read field annotations and values (via reflection) and create `ConditionBuffer` fragments.
4. `JoinBuffer` collects JOIN clauses and ensures duplicates are not emitted; builders call `joinBuffer.processJoinSearchProperty(...)` when they need the join to be added/considered.
5. `QueryParameterizer` walks the search DTO and binds all parameter values to the JPA `Query` using generated parameter names from `ParameterCounter`.

Example: defining a search DTO
-----------------------------
Assume a `Catalog` entity that has a collection `products` and a `name` property. A corresponding search DTO may look like:

```java
@SearchForClass(value = Catalog.class, distinct = true)
public class CatalogSearch implements Search {

    @SearchProperty(value = "name", isLike = true, concat = @Concat({"name", "description"}))
    private String q;

    @SearchProperty(value = "products", isLike = false, join = @Join(value = "c.products p", left = true, fetch = true))
    private List<Product> products;

    // getters/setters
}
```

Concrete examples (entities, DTOs, test)
----------------------------------------
Below are small, copy-pastable snippets you can use as a starting point. They are intentionally minimal — adapt mapping details to your domain.

Entity: `Catalog`

```java
package io.github.spring.middleware.jpa.buffer.testentities;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "catalogs")
public class Catalog {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "catalog", cascade = CascadeType.ALL)
    private List<Product> products;

    // constructors, getters and setters
}
```

Entity: `Product`

```java
package io.github.spring.middleware.jpa.buffer.testentities;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private UUID id;

    @Column(name = "name")
    private String name;

    @ManyToOne
    @JoinColumn(name = "catalog_id")
    private Catalog catalog;

    // constructors, getters and setters
}
```

Search DTO: `CatalogSearch` (multi-arg / concat example)

```java
package io.github.spring.middleware.jpa.buffer.testentities;

import io.github.spring.middleware.jpa.annotations.Concat;
import io.github.spring.middleware.jpa.annotations.Join;
import io.github.spring.middleware.jpa.annotations.SearchForClass;
import io.github.spring.middleware.jpa.annotations.SearchProperty;
import io.github.spring.middleware.jpa.search.Search;

import java.util.List;
import java.util.UUID;

@SearchForClass(value = Catalog.class, distinct = true)
public class CatalogSearch implements Search {

    @SearchProperty(value = "name", isLike = true, concat = @Concat({"name", "description"}))
    private String q;

    // Example: search by referenced products (join is declared so join is added once)
    @SearchProperty(value = "products", isLike = false, join = @Join(value = "c.products p", left = true, fetch = true))
    private List<UUID> productIds;

    // getters / setters
}
```

Search DTO: `ProductSearch` (multi-argument example)

```java
package io.github.spring.middleware.jpa.buffer.testentities;

import io.github.spring.middleware.jpa.annotations.SearchForClass;
import io.github.spring.middleware.jpa.annotations.SearchProperty;
import io.github.spring.middleware.jpa.search.Search;

@SearchForClass(value = Product.class)
public class ProductSearch implements Search {

    @SearchProperty(value = "name", isLike = true)
    private String name;

    @SearchProperty(value = "catalog.id", isLike = false)
    private java.util.UUID catalogId;

    // getters / setters
}
```

Repository usage (interface)

```java
public interface CatalogSearchRepository extends SearchRepository<Catalog, CatalogSearch> {
}

// then in service:
// List<Catalog> r = catalogSearchRepository.findBySearch(searchDto, pagination);
```

Quick unit test showing QueryBuffer generation

```java
@Test
public void buildsCatalogQuery_joinAndConcat_present() {
    CatalogSearch s = new CatalogSearch();
    s.setQ("summer");
    s.setProductIds(java.util.Collections.singletonList(UUID.randomUUID()));

    QueryBufferParameters<Catalog, io.github.spring.middleware.jpa.search.Search> params =
            new QueryBufferParameters<>(s, Catalog.class, new io.github.spring.middleware.jpa.order.OrderBy(), false);
    QueryBuffer<Catalog, io.github.spring.middleware.jpa.search.Search> qb = new QueryBuffer<>(params);
    String query = qb.toString();

    // assertions: join appears, concat expression appears and param placeholders exist
    org.junit.jupiter.api.Assertions.assertTrue(query.contains("LEFT JOIN") || query.contains("JOIN"));
    org.junit.jupiter.api.Assertions.assertTrue(query.toUpperCase().contains("UPPER(CONCAT") || query.contains("c.products"));
}
```

Generated JPQL (conceptual) for a non-null `q` and `productIds` could be:

```
SELECT DISTINCT c FROM Catalog c LEFT JOIN FETCH c.products p
 WHERE (UPPER(CONCAT(c.name,' ',c.description)) LIKE :param0) AND (c.products IN :param1)
 ORDER BY c.name
```

Pagination & ordering
---------------------
- `SearchRepositoryImpl` wires `OrderBy` into `QueryBufferParameters` so that `QueryBuffer` appends an `ORDER BY` clause.
- Pagination is applied via `setFirstResult` and `setMaxResults` on the `Query` when no post-filters are required.
- If `QueryFilter` post-filters are used (custom in-memory filtering after database fetch), pagination may be applied afterwards using `PaginableResultDB`.

Extending the condition builders
--------------------------------
The framework resolves how to build a condition for a field by locating an annotation whose annotation type is itself annotated with `@ConditionBufferBuilderClass`. That annotation's `value()` returns a `ConditionBufferBuilder` implementation class. The `ConditionBufferBuilderFactory` uses the Spring `ApplicationContext` to obtain an instance and sets `JoinBuffer`/`ParameterCounter` on it.

This allows adding new domain-specific annotations and small builder implementations when the supplied builders do not cover a use-case.

Testing tips
------------
- The query generation is deterministic and easily unit-testable: instantiate a `QueryBuffer` with `QueryBufferParameters` and assert the produced JPQL string.
- The project contains comprehensive tests for the buffer builders under `parent/jpa/core/src/test/java/...` — use those as patterns to test new annotations, joins and pre-conditions.
- When testing builders that rely on Spring beans (factory), provide a mock `ApplicationContext` that returns concrete builder instances as the tests in the project do.

Best practices
--------------
- Keep search DTOs small and focused: every field you expose can contribute to more complex JPQL.
- Prefer using pagination for queries that may return many rows.
- Use `@Join` metadata to control join strategy (LEFT/RIGHT/FETCH) and to avoid N+1 problems when appropriate.
- If you need custom condition logic, add a small `ConditionBufferBuilder` and annotate a meta-annotation with `@ConditionBufferBuilderClass`.

Where to look in the code
-------------------------
- `parent/jpa/core/src/main/java/io/github/spring/middleware/jpa/repository/SearchRepositoryImpl.java`
- `parent/jpa/core/src/main/java/io/github/spring/middleware/jpa/buffer/` (all buffer classes)
- `parent/jpa/core/src/main/java/io/github/spring/middleware/jpa/buffer/builder/` (concrete builders)
- `parent/jpa/core/src/test/java/io/github/spring/middleware/jpa/buffer/` (tests that validate builder behavior)

Changelog / notes
-----------------
- This documentation was expanded after adding comprehensive buffer tests which demonstrate join deduplication, LEFT/RIGHT/FETCH joins, concat, pre-conditions and sub-search embedding.

## Related documentation

- [README.md](../README.md) — high-level project overview
- [Architecture](./architecture.md)
- [Communication](./communication.md)
- [Errors](./errors.md)
- [GraphQL](./graphql.md)
- [Kafka](./kafka.md)
- [RabbitMQ](./rabbitmq.md)
- [Redis](./redis.md)
- [Mongo](./mongo.md)
- [Logging](./logging.md)
- [Security](./security.md)
- [Core](./core.md)
- [Client Security](./client-security.md)
- [Client Resilience](./client-resilience.md)
