package com.core.redis.unit;

public interface RedisUnit<K extends RedisUnitKey> {

    K getKey();

}
