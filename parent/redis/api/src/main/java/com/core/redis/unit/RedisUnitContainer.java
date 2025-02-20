package com.core.redis.unit;

import com.core.redis.RedisKey;

import java.util.Collection;

public interface RedisUnitContainer<R extends RedisKey, U extends RedisUnit> {

    R getRedisKey();

    Collection<U> getRedisUnits();

}
