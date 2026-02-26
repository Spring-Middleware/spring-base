package com.middleware.redis.unit;

public interface RedisUnit<K extends RedisUnitKey> {

    K getKey();

}
