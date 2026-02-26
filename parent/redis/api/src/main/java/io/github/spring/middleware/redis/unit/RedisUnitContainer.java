package io.github.spring.middleware.redis.unit;

import io.github.spring.middleware.redis.RedisKey;

import java.util.Collection;

public interface RedisUnitContainer<R extends RedisKey, U extends RedisUnit> {

    R getRedisKey();

    Collection<U> getRedisUnits();

}
