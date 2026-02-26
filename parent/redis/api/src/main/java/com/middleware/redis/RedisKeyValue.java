package com.middleware.redis;

public interface RedisKeyValue<K extends RedisKey, V extends RedisValue> {

    K getRedisKey();

    V getRedisValue();


}
