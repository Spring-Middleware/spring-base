# Redis Module

Overview
--------
The Redis module provides low-level connection management and higher-level helpers for distributed locks, mutexes, and typed key/value operations.

Key concepts
------------
- `RedisConnectionParameters`: `@ConfigurationProperties(prefix = "redis")` used to configure host, port, pool and cluster options.
- `RedisService`: generic service API for typed key/value operations using custom key/value converters.
- `RedisLock` / `RedisLockFactory` / `RedisMutex`: utilities for distributed locking.

Configuration
-------------
The module reads configuration from properties under the `redis` prefix. Minimal example:

```yaml
redis:
  host: ${REDIS_HOST:localhost}
  port: ${REDIS_PORT:6379}
  database: 0
  timeout: 2000
  max-pool-conn: 10
  max-idle-pool-conn: 10
  min-idle-pool-conn: 1
  max-wait-millis: 1000
  is-cluster: false
```

(See `RedisConnectionParameters` for all available fields and defaults.)

Usage
-----
- Implement `RedisKey`, `RedisValue` and `RedisKeyValue` for your domain types.
- Use `RedisService` to read/write/delete typed key/value pairs with provided functional converters.
- Use `RedisLockFactory` to obtain distributed locks when performing multi-step updates.

Examples
--------
```text
Example usage (conceptual):
- Autowire `RedisService<MyKey, MyValue, MyKeyValue>` into your component.
- Build a collection of keys (List<MyKey> keys = ...).
- Call `redisService.getKeyValues(keys, MyGetFunctions.INSTANCE)` to retrieve typed key/value pairs.
```

Notes and best practices
------------------------
- Use the pool settings to tune for your environment.
- Prefer small, focused Redis keys and keep payloads compact.
- Use the locking utilities when you need strong coordination across nodes.

Where to look in the codebase
----------------------------
- `parent/redis/core/src/main/java/io/github/spring/middleware/redis/config/RedisConnectionParameters.java`
- `parent/redis/core/src/main/java/io/github/spring/middleware/redis/service/RedisServiceImpl.java`
- `parent/redis/core/src/main/java/io/github/spring/middleware/redis/service/RedisLockFactory.java`

Further reading
---------------
See module `parent/redis` for additional packages (`api`, `core-react`) and examples.

---

## Related documentation

- `README.md` — project overview.
- `docs/architecture.md` — architecture and control/data plane.
- `docs/communication.md` — service communication and declarative clients.
- `docs/errors.md` — unified error model.
- `docs/registry.md` — registry and schema location metadata.
- `docs/graphql.md` — GraphQL support.
- `docs/kafka.md` — Kafka integration.
- `docs/client-security.md` — security for declarative clients.
- `docs/logging.md` — request/response logging.
- `docs/redis.md` — Redis module (this document).
- `docs/mongo.md` — Mongo module.
- `docs/jpa.md` — JPA module.
- `docs/rabbitmq.md` — RabbitMQ module.
- `docs/security.md` — HTTP security configuration.
