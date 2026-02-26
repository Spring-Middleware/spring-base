package io.github.spring.middleware.redis;

@FunctionalInterface
public interface ToValueFunction<K extends RedisKey, V extends RedisValue> {

    V apply(K key, String value);

}
