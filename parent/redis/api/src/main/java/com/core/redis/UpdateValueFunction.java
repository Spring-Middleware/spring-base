package com.core.redis;

@FunctionalInterface
public interface UpdateValueFunction<K extends RedisKey, V extends RedisValue> {

    V apply(K redisKey, V redisValue);

}
