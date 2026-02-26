package com.middleware.redis;

@FunctionalInterface
public interface ToKeyValueFunction<K extends RedisKey, V extends RedisValue, KV extends RedisKeyValue<K, V>> {

    KV apply(K key, V value);
}
