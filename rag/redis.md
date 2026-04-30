# Redis Module (RAG-Friendly)

## Quick Answer

**How do I configure Redis in Spring Middleware?**
Configuration is read using the `redis` prefix.

**Complete Example (`application.yml`):**
```yaml
redis:
  host: localhost
  port: 6379
  database: 0
  timeout: 2000
```

**Constraints:**
- The configuration prefix is `redis`.
- The connection options use `@ConfigurationProperties(prefix = "redis")`.

---

## Redis Usage

### How do I store and retrieve typed key/value pairs?
Use `RedisService` to interact programmatically with your customized Redis cluster.

**Constraints:**
- `RedisService` provides a generic service API utilizing custom key/value converters injected at runtime.
- For generic operations, reference `RedisServiceImpl`.

---

## Distributed Locks

### How do I use distributed locks with Redis?
Distributed locking mechanisms are available through `RedisLock`, `RedisLockFactory`, and `RedisMutex`.

**Java concept:**
Use the `RedisLockFactory` to acquire locks across your microservices platform. Refer to the module code under `parent/redis/core/src/main/java/io/github/spring/middleware/redis/service/RedisLockFactory.java`.
