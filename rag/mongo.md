# Mongo Module (RAG-Friendly)

## Quick Answer

**How do I perform dynamic searches in MongoDB?**
Define a DTO that implements `MongoSearch`, create a repository interface that extends `MongoSearchRepository<T, S>`, and call `findBySearch()`.

**Java code:**
```java
// 1. Define the Search DTO
import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ProductSearch implements MongoSearch {
    private String name;
    private String status;
}

// 2. Create the Repository Interface
public interface ProductSearchRepository extends MongoSearchRepository<Product, ProductSearch> {
    // Inherits findBySearch, aggregateBySearch, and countBySearch
}

// 3. Use the Repository
@Service
public class ProductService {
    private final ProductSearchRepository repository;

    public ProductService(ProductSearchRepository repository) {
        this.repository = repository;
    }

    public Page<Product> searchProducts(ProductSearch searchDto, Pagination pagination) {
         // Performs query dynamically based on the non-null fields in searchDto
         return repository.findBySearch(searchDto, pagination);
    }
}
```

**Constraints:**
- The repository must extend `MongoSearchRepository`.
- The search DTO must implement `MongoSearch`.
- The pagination object MUST be strongly typed using the module's `Pagination` DTO.

---

## Dynamic Query Generation

### How are search properties translated to MongoDB Queries?
The `CriteriaBuilderComponent` inspects properties of the `MongoSearch` DTO and automatically generates a MongoDB `$and` query containing conditions for each non-null property.

**Java DTO:**
```java
@Data
public class UserSearch implements MongoSearch {
    private String key;
    private String value;
}
```

**Generated MongoDB Query when both fields are populated:**
```json
{
  "$and": [ 
    { "value": "VALUE" }, 
    { "key": "KEY" } 
  ]
}
```

### How do I map nested properties?
If a root search DTO contains a nested search object, the translator creates dot-notated fields inside an `$and` block.

**Java DTO:**
```java
@Data
public class RootSearch implements MongoSearch {
    private String key;
    private SubSearch subSearch;
}

@Data
public class SubSearch {
    private String propA;
    private String propB;
}
```

**Generated MongoDB Query:**
```json
{
  "$and": [
    { "$and": [ { "subSearch.propA": "PROP_A" }, { "subSearch.propB": "PROP_B" } ] },
    { "key": "KEY" }
  ]
}
```

---

## Collections and Elements Matching

### How do I query elements inside a collection or array?
When searching against a property that is a collection (like a List of items), the module automatically builds an `$elemMatch` condition.

**Generated MongoDB Query for List matching:**
```json
{
  "$and": [
    { "itemSearch": { 
        "$elemMatch": { 
            "$and": [ 
                { "propItemA": "PROP_ITEM_A" }, 
                { "propItemB": "PROP_ITEM_B" } 
            ] 
        } 
    } }
  ]
}
```

**Constraints:**
- `$elemMatch` ensures that at least one array element matches ALL criteria defined in the sub-search object.

---

## Aggregation Pipelines

### How do I run dynamic aggregations instead of queries?
You can use `aggregateBySearch()` in your `MongoSearchRepository`. To compute derived fields directly in MongoDB (e.g., concatenating fields), the `AddFieldsBuilderComponent` creates an `$addFields` stage.

**Usage in Service:**
```java
public Object runAggregation(ProductSearch searchDto) {
    // Generates an aggregation pipeline based on the DTO configuration
    return repository.aggregateBySearch(searchDto);
}
```

**Example Generated Pipeline Stage for `$concat`:**
```json
{
  "$addFields": {
    "itemSearch": {
      "$map": {
        "input": "$itemSearch",
        "as": "u",
        "in": {
          "propAB": { "$concat": ["$$u.propA", " ", "$$u.propB"] },
          "propItemA": "$$u.propItemA"
        }
      }
    }
  }
}
```

**Constraints:**
- Derived attributes and advanced aggregations (`$group`, `$sort`, `$project`) heavily rely on property annotations linked to the module's processors.
- Limit usage of heavy `$addFields` queries against large collections avoiding performance issues.
