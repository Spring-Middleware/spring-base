# Mongo Module

Overview
--------
The Mongo module provides dynamic search and aggregation helpers on top of Spring Data MongoDB. It exposes a repository base implementation and components that build queries and aggregation pipelines from typed search DTOs.

What it offers
--------------
- `MongoSearchRepositoryImpl` for dynamic find, count and aggregate operations (including geo queries).
- `QueryCreatorComponent`, `CriteriaBuilderComponent`, `SortBuilderComponent`, `PipeLineBuilderComponent` — translator components that convert a `MongoSearch` DTO into `Query`/`Aggregation` objects.
- Pagination support via a `Pagination` DTO and result helpers.

Quick usage
-----------
- Define a `MongoSearch` DTO for your domain.
- Create a repository interface extending `MongoSearchRepository<T, S>`.
- Use `findBySearch`, `aggregateBySearch` and `countBySearch` from the repository.

Example
-------
```java
public interface ProductSearchRepository extends MongoSearchRepository<Product, ProductMongoSearch> {
}

List<Product> results = productSearchRepository.findBySearch(searchDto, pagination);
```

Notes and best practices
------------------------
- Aggregations are built from the search DTO; for complex aggregations review the pipeline builder to ensure intended stages.
- Use geo-search helpers for location-based queries.
- Validate search DTOs to avoid generating heavy queries unintentionally.

Where to look
-------------
- `parent/mongo/core/src/main/java/io/github/spring/middleware/mongo/repository/MongoSearchRepositoryImpl.java`
- `parent/mongo/core/src/main/java/io/github/spring/middleware/mongo/components`

Further reading
---------------
See the `parent/mongo` module sources for examples and advanced options.

---

## Related documentation

- `README.md` — high-level project overview.
- `docs/architecture.md` — architecture and control/data plane.
- `docs/communication.md` — service communication and declarative clients.
- `docs/errors.md` — unified error model and propagation.
- `docs/registry.md` — registry and schema location metadata.
- `docs/graphql.md` — GraphQL support.
- `docs/kafka.md` — Kafka integration.
- `docs/client-security.md` — security for declarative clients.
- `docs/logging.md` — request/response logging.
- `docs/redis.md` — Redis module.
- `docs/mongo.md` — Mongo module (this document).
- `docs/jpa.md` — JPA module.
- `docs/rabbitmq.md` — RabbitMQ module.
- `docs/security.md` — HTTP security configuration.
