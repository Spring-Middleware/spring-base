# Cache Module (RAG-Friendly)

## Quick Answer

**How do I configure Redis cache TTLs in my Spring application?**
Use the `@RedisCacheConfiguration` annotation alongside Spring's standard `@Cacheable`.

**Java code:**
```java
import org.springframework.cache.annotation.Cacheable;
import io.github.spring.middleware.cache.annotations.RedisCacheConfiguration;

@Service
public class CatalogService {

    @Cacheable("catalog-events")
    @RedisCacheConfiguration(ttlString = "${CACHE_TTL_SECONDS:300}", chronoUnitString = "SECONDS")
    public List<Item> findByCatalogId(UUID catalogId) {
        // Method execution logic...
        return new ArrayList<>();
    }
}
```

**YAML Configuration required:**
```yaml
spring:
  cache:
    type: redis
    redis:
      enable-statistics: true  # Enables CacheController metrics
```

**Constraints:**
- You MUST also include Spring's `@Cacheable("your-cache-name")` for `@RedisCacheConfiguration` to be processed properly by `RedisCacheConfigurationSupport`.
- When both `ttl` and `ttlString` are provided, `ttlString` takes precedence after resolving placeholders.

---

## Cache Management & Statistics

### How do I programmatically clear a cache or view its statistics?
Inject the `CacheService` bean to manage caches programmatically. The module also exposes REST endpoints via `CacheController`.

**Java code:**
```java
@Autowired
private CacheService cacheService;

public void refreshCache() {
    // Clear cache
    boolean success = cacheService.clearCache("catalog-events");
    
    // Get statistics
    CacheInformationStadisticsFilter filter = CacheInformationStadisticsFilter.builder()
        .cacheNames(List.of("catalog-events"))
        .build();
    Collection<CacheInformationStatistics> stats = cacheService.getCacheInformationStadistics(filter);
}
```

### What are the available REST endpoints?
The `CacheController` provides three endpoints under `/cache`:
- `GET /cache/stadistics?cacheNames=X` – Returns hit/miss metrics (`CacheInformationStatistics`).
- `GET /cache/config?cacheNames=X` – Returns configured TTL parameters.
- `GET /cache/clear?cacheName=X` – Clears the specified cache completely.

**Constraints:**
- To capture hits, misses, and puts, you MUST enable `spring.cache.redis.enable-statistics=true` in your properties so Micrometer meters (`cache.gets`/`cache.puts`) are emitted.
