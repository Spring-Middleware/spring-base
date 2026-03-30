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

Examples and generated queries
-----------------------------
The module contains unit tests that demonstrate the mapping of `MongoSearch` DTO fields into MongoDB `Criteria` and `Aggregation` JSON. Below are representative examples extracted from the tests (they are reproduced here as examples of what the components generate):

1) Simple equality on a property

Search DTO: set `key = "KEY"` -> Generated query JSON:
```json
{
  "$and": [ { "key": "KEY" } ]
}
```

2) Combined conditions (value + key)

Search DTO: set `value = "VALUE"`, `key = "KEY"` -> Generated query JSON:
```json
{
  "$and": [ { "value": "VALUE" }, { "key": "KEY" } ]
}
```

3) Nested sub-search (class property mapping)

If your search DTO contains a nested search `subSearch` with properties `propA` and `propB`, the component generates an `$and` with dot-notated fields:
```json
{
  "$and": [
    { "$and": [ { "subSearch.propA": "PROP_A" }, { "subSearch.propB": "PROP_B" } ] },
    { "key": "KEY" }
  ]
}
```

4) OR conditions

Two search properties `orA` and `orB` configured as OR produce an `$or` clause:
```json
{
  "$or": [ { "orB": "OR_B" }, { "orA": "OR_A" } ]
}
```

5) Nested arrays with `$elemMatch`

When the search matches elements inside a collection property (e.g. `itemSearch`), the component uses `$elemMatch` and composes the internal criteria with `$and`:
```json
{
  "$and": [
    { "itemSearch": { "$elemMatch": { "$and": [ { "propItemA": "PROP_ITEM_A" }, { "propItemB": "PROP_ITEM_B" } ] } } }
  ]
}
```

6) `AddFields` example (concatenation)

The `AddFieldsBuilderComponent` can build aggregation stages that compute derived fields. Example of `$addFields` that concatenates two sub-fields into `propAB`:
```json
{
  "$addFields": {
    "itemSearch": {
      "$map": {
        "input": "$itemSearch",
        "as": "u",
        "in": {
          "propAB": { "$concat": ["$$u.propA", " ", "$$u.propB"] },
          "propItemA": "$$u.propItemA",
          "class": "$$u.class",
          "propItemD": "$$u.propItemD",
          "propItemC": "$$u.propItemC",
          "propItemB": "$$u.propItemB"
        }
      }
    }
  }
}
```

How it works
------------
The module uses a small processing pipeline that inspects `MongoSearch` DTOs and translates annotated properties into MongoDB `Criteria`/`Aggregation` builders:

- `MongoSearchPropertyProcessor` handles property-level processing and annotation semantics (e.g. mapping a field to equality, range, OR/AND, geo, etc.).
- `MongoSearchClassProcessor` orchestrates class-level translation and delegates to `CriteriaBuilderComponent` to compose nested criteria.
- `MongoSearchPropertiesProcessor` groups property processors when needed.
- `CriteriaBuilderComponent` builds `Criteria` objects applied to a `Query`.
- `AddFieldsBuilderComponent` builds `$addFields` aggregation stages for derived fields (concats, maps over arrays, etc.).

The test-suite contains concrete examples (see `parent/mongo/core-commons/src/test/java` and the JSON resources under `src/test/resources`) showing the expected JSON representation of generated queries and aggregation stages.

Pagination and aggregation
--------------------------
- The repository helpers support pagination (a `Pagination` DTO and result wrappers). Use the provided pagination DTO when calling `findBySearch(...)`.
- Aggregations are constructed from the search DTO and the pipeline builder. You can combine `$addFields`, `$match`, `$group`, `$project`, `$sort`, `$skip` and `$limit` according to your `MongoSearch` configuration.

Geo queries
-----------
The module includes helpers for geo queries. If your `MongoSearch` DTO contains geo-related fields (e.g., radius, location coordinates), the property processors generate `nearSphere`/`geoWithin` criteria as appropriate. Consult the `MongoSearchPropertyProcessor` for the exact mapping logic.

Integration and extending
-------------------------
- Create a search DTO implementing or extending `MongoSearch` with fields annotated (the module tests show examples of naming and composition).
- Extend or override `CriteriaBuilderComponent`, `AddFieldsBuilderComponent`, or processors to add custom operators or mappings for project-specific needs.
- `MongoSearchRepositoryImpl` exposes high-level methods `findBySearch`, `aggregateBySearch`, `countBySearch` that you can use directly from your Spring Data repositories.

Where to look (important classes)
---------------------------------
- `parent/mongo/core/src/main/java/io/github/spring/middleware/mongo/repository/MongoSearchRepositoryImpl.java`
- `parent/mongo/core-commons/src/main/java/io/github/spring/middleware/mongo/processor` (processors)
- `parent/mongo/core-commons/src/main/java/io/github/spring/middleware/mongo/components` (builder components)
- Tests and JSON resources:
  - `parent/mongo/core-commons/src/test/java/io/github/spring/middleware/mongo` (component tests)
  - `parent/mongo/core-commons/src/test/resources/*.json` (expected query JSON examples)

Examples (code snippet)
-----------------------
Define a search DTO (simplified example):

```text
// Example ProductSearch DTO (pseudocode)
@Data
@Builder
public class ProductSearch implements MongoSearch {
    private String key;
    private String name;
    private GeoPoint location; // hypothetical
    private List<ItemSearch> itemsSearch; // nested
}
```

Repository interface:

```text
// Example repository interface (pseudocode)
public interface ProductSearchRepository extends MongoSearchRepository<Product, ProductSearch> {
}
```

Usage in service:

```text
// Example usage in a service (pseudocode):
@Autowired
private ProductSearchRepository productSearchRepository;

Pagination pagination = new Pagination(0, 20);
// call findBySearch(searchDto, pagination) on your injected repository
Page<Product> page = productSearchRepository.findBySearch(searchDto, pagination);
```

Best practices
--------------
- Keep search DTOs focused and avoid adding many unconstrained fields that could generate heavy queries.
- Validate incoming search DTOs to prevent performance issues (for example: max page size, sane radius values for geo queries).
- Use `$addFields` and computed fields sparingly — they are powerful but can increase aggregation complexity.

---

## Related documentation

- [README.md](../README.md)
- [Getting Started](./getting-started.md)
- [Architecture](./architecture.md)
- [Communication](./communication.md)
- [Errors](./errors.md)
- [Registry](./registry.md)
- [Kafka](./kafka.md)
- [Client Security](./client-security.md)
- [Logging](./logging.md)
- [Redis](./redis.md)
- [JPA](./jpa.md)
- [RabbitMQ](./rabbitmq.md)
- [Security](./security.md)
- [Core](./core.md)
