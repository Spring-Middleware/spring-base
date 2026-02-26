package io.github.spring.middleware.redis.unit;

public interface RedisUnit<K extends RedisUnitKey> {

    K getKey();

}
