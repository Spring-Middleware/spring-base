# JPA Module

Overview
--------
The JPA module provides dynamic search and query utilities built on top of JPA/Hibernate. It exposes a repository base implementation that constructs JPQL queries from typed search DTOs, and supports pagination, ordering, and optional post-filtering.

Key components
--------------
- `SearchRepositoryImpl` — base repository implementation that uses `EntityManager` to build and execute dynamic JPQL queries.
- `QueryBuffer`, `QueryBufferParameters`, `QueryParameterizer` — helpers that assemble parameterized JPQL strings and set query parameters.
- Pagination and ordering helpers to support paging and ordering at the repository level.

Quick usage
-----------
- Define a `Search` DTO that describes filter properties and optional ordering.
- Create a repository interface extending `SearchRepository<T, S>` for your entity and search DTO.
- Use `findBySearch(searchDto, pagination)` and `countBySearch(searchDto)` to run queries.

Example
-------
```java
public interface ProductSearchRepository extends SearchRepository<Product, ProductSearch> {
}

// use
List<Product> results = productSearchRepository.findBySearch(productSearchDto, pagination);
long total = productSearchRepository.countBySearch(productSearchDto);
```

Notes & best practices
----------------------
- The implementation generates JPQL; keep your search DTO fields focused to avoid expensive generated queries.
- Use pagination (page size) to avoid large result sets.
- The module provides a buffer/query parameterization approach to keep queries safe and testable.

Where to look
-------------
- `parent/jpa/core/src/main/java/io/github/spring/middleware/jpa/repository/SearchRepositoryImpl.java`
- `parent/jpa/core/src/main/java/io/github/spring/middleware/jpa/query` and `.../buffer` packages

Further reading
---------------
Inspect `parent/jpa` module sources for examples and advanced usage.
