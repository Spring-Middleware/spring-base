# JPA Module (RAG-Friendly)

## Quick Answer

**How do I perform dynamic searches in JPA?**
Define a DTO annotated with `@SearchProperty`, create a repository interface that extends `SearchRepository<T, S>`, and call `findBySearch()`.

**Java code:**
```java
// 1. Define the Search DTO
import io.github.spring.middleware.jpa.annotations.SearchForClass;
import io.github.spring.middleware.jpa.annotations.SearchProperty;
import io.github.spring.middleware.jpa.search.Search;

@SearchForClass(value = Catalog.class, distinct = true)
public class CatalogSearch implements Search {
    
    @SearchProperty(value = "name", isLike = true)
    private String name;
    
    // getters/setters
}

// 2. Create the Repository Interface
import io.github.spring.middleware.jpa.repository.SearchRepository;

public interface CatalogSearchRepository extends SearchRepository<Catalog, CatalogSearch> {
    // Inherits findBySearch and other dynamic methods
}

// 3. Use the Repository
@Service
public class CatalogService {
    private final CatalogSearchRepository repository;

    public CatalogService(CatalogSearchRepository repository) {
        this.repository = repository;
    }

    public List<Catalog> searchCatalogs(CatalogSearch searchDto, Pagination pagination) {
         // Generates JPQL dynamically based on populated fields
         return repository.findBySearch(searchDto, pagination).getContent();
    }
}
```

**Constraints:**
- The repository MUST extend `SearchRepository`.
- The search DTO MUST implement the `Search` interface and be annotated with `@SearchForClass`.

---

## Multiple Fields & Concatenation

### How do I search across multiple fields with a single string?
Use the `concat` property inside `@SearchProperty` with the `@Concat` annotation.

**Java DTO:**
```java
@SearchForClass(value = Catalog.class)
public class CatalogSearch implements Search {

    @SearchProperty(value = "name", isLike = true, concat = @Concat({"name", "description"}))
    private String q;
}
```

**Generated JPQL fragment:**
```jpql
(UPPER(CONCAT(c.name,' ',c.description)) LIKE :param0)
```

**Constraints:**
- When `isLike` is `true`, it automatically creates an `UPPER() LIKE` condition for case-insensitive partial matching.

---

## Joins and Relationships

### How do I filter by properties of related entities?
Use the `join` property inside `@SearchProperty` with the `@Join` annotation to add the necessary JOIN clause to the generated JPQL without causing duplicates.

**Java DTO:**
```java
@SearchForClass(value = Catalog.class, distinct = true)
public class CatalogSearch implements Search {

    @SearchProperty(
        value = "products", 
        isLike = false, 
        join = @Join(value = "c.products p", left = true, fetch = true)
    )
    private List<UUID> productIds;
}
```

**Generated JPQL fragment:**
```jpql
SELECT DISTINCT c FROM Catalog c LEFT JOIN FETCH c.products p WHERE (c.products IN :param1)
```

**Constraints:**
- The `join` annotation manages deduplication internally; if multiple search properties declare the exact same join value, it is only appended once.
- `left = true` generates a `LEFT JOIN`.
- `fetch = true` adds the `FETCH` keyword to prevent N+1 problems.

---

## Nested Searches

### How do I nest sub-searches inside another search?
Use the `@SubSearch` annotation on a field that contains another DTO implementing `Search`.

**Java DTO:**
```java
@SearchForClass(value = Order.class)
public class OrderSearch implements Search {

    @SearchProperty(value = "status")
    private String status;

    @SubSearch
    private CustomerSearch customerSettings;
}
```

**Constraints:**
- The framework embeds the sub-search WHERE fragment seamlessly into the parent query using `AND`.
