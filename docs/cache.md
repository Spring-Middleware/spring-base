Cache module
============

Overview
--------
The cache module adds Redis-based caching helpers and runtime introspection for Spring applications. It provides:

- Annotation-driven cache TTL configuration via `@RedisCacheConfiguration` (method-level).
- Automatic registration of Redis cache configurations (used by the RedisCacheManager builder).
- A small HTTP controller to inspect cache configuration and runtime statistics.
- Integration with Micrometer metrics for cache hits/misses/puts.
- A `CacheService` to clear caches, list runtime statistics and configured caches.

Key classes
-----------
- `io.github.spring.middleware.cache.annotations.RedisCacheConfiguration` — method-level annotation to declare TTL and time unit for caches declared with `@Cacheable`.
- `io.github.spring.middleware.cache.config.RedisCacheConfigurationSupport` — scans application methods annotated with `@Cacheable` + `@RedisCacheConfiguration`, builds `RedisCacheConfigurationParameters` and exposes a `RedisCacheManagerBuilderCustomizer` bean. Also provides `getSpecificRedisCacheConfigurations()` for modules to add extra configs.
- `io.github.spring.middleware.cache.service.CacheService` / `CacheServiceImpl` — programmatic API to clear caches and read cache metrics.
- `io.github.spring.middleware.cache.controller.CacheController` — REST endpoints for runtime configuration and statistics.
- `io.github.spring.middleware.cache.data.CacheInformationStatistics` — DTO with fields: `cacheName, hit, miss, pending, puts`.

How it works
------------
1. Annotate your caching methods with Spring's `@Cacheable("my-cache")` as usual.
2. Optionally, annotate the same method with `@RedisCacheConfiguration(ttl=..., chronoUnit=...)` to provide TTL (time-to-live) used when building Redis cache configurations.
3. At startup, `RedisCacheConfigurationSupport` uses reflection to find methods annotated with `@Cacheable` and `@RedisCacheConfiguration`, resolves TTL (including placeholder strings using `ttlString` / `chronoUnitString`) and registers per-cache configurations in the RedisCacheManager builder via `redisCacheManagerBuilderCustomizer()` bean.
4. If `spring.cache.redis.enable-statistics=true` the Redis cache manager will enable statistics and Micrometer meters named `cache.gets` and `cache.puts` are used. `CacheServiceImpl` reads meters and builds `CacheInformationStatistics` objects.

Configuration
-------------
Minimal example in `application.yml` (Redis configuration is assumed present separately):

spring:
  cache:
    type: redis
    redis:
      enable-statistics: true

Example: use placeholder strings (recommended for environment-driven values):

public @Cacheable("catalog-events")
@RedisCacheConfiguration(ttlString = "${CACHE_TTL_SECONDS:300}", chronoUnitString = "SECONDS")
List<Item> findByCatalogId(UUID catalogId) { ... }

`@RedisCacheConfiguration` properties:
- `ttl` (int) — numeric TTL (default 0)
- `chronoUnit` (ChronoUnit) — time unit (default MINUTES)
- `ttlString` (String) — alternative to `ttl` that accepts placeholders (e.g. `${CACHE_TTL}`)
- `chronoUnitString` (String) — alternative to `chronoUnit` that accepts placeholders

Notes: when both `ttlString` and `ttl` are present, `ttlString` takes precedence after resolving placeholders.

Controller endpoints
--------------------
The module exposes a small REST API under `/cache` (controller: `CacheController`):

- GET `/cache/stadistics?cacheNames=` — returns a collection of `CacheInformationStatistics` objects filtered by optional `cacheNames` query parameter.
- GET `/cache/config?cacheNames=` — returns current `RedisCacheConfigurationParameters` known (configured TTLs) filtered by optional `cacheNames`.
- GET `/cache/clear?cacheName=` — clears the cache with the given name (returns boolean success).

Programmatic API
----------------
Use the `CacheService` bean to interact with caches programmatically:

@Autowired
private CacheService cacheService;

cacheService.clearCache("catalog-events");
Collection<CacheInformationStatistics> stats = cacheService.getCacheInformationStadistics(
    CacheInformationStadisticsFilter.builder().cacheNames(List.of("catalog-events")).build());

Extending and advanced usage
----------------------------
- `RedisCacheConfigurationSupport#getSpecificRedisCacheConfigurations()` can be overridden by framework modules to add extra cache configurations programmatically.
- The module registers a custom `GenericJackson2JsonRedisSerializer` backed by an `ObjectMapper` configured with JDK8 and JavaTime modules and polymorphic typing enabled — this allows storing complex objects in Redis without additional configuration.
- Metrics: the implementation expects meters named `cache.gets` and `cache.puts`. The `CacheServiceImpl` reads these meters and maps tags `name` and `result` to populate the `CacheInformationStatistics` fields (`hit`, `miss`, `pending`, `puts`).

Best practices
--------------
- Prefer `ttlString` with environment placeholders in production so values are configurable without code changes.
- Use descriptive cache names and avoid mixing responsibilities in a single cache.
- Enable `spring.cache.redis.enable-statistics` only when you need runtime statistics (it carries a slight cost).
- When storing polymorphic types in cache prefer the default JSON serializer provided by this module. If you need custom serialization, provide your own `RedisCacheManagerBuilderCustomizer` or override the serializer bean.

Linking and references
----------------------
This document is also referenced from the repository README (see `docs/cache.md`).


