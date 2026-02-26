package com.middleware.redis.unit;

import com.middleware.redis.RedisKey;

import java.util.Collection;

public interface RedisUnitContainer<R extends RedisKey, U extends RedisUnit> {

    R getRedisKey();

    Collection<U> getRedisUnits();

}
