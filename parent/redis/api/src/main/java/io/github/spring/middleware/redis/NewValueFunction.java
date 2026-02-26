package io.github.spring.middleware.redis;

@FunctionalInterface
public interface NewValueFunction<K extends RedisKey, V extends RedisValue> {

    V apply(K key);

}
