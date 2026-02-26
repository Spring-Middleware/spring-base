package com.middleware.redis.component;

import com.middleware.redis.RedisKey;
import com.middleware.redis.unit.RedisUnit;
import com.middleware.redis.unit.RedisUnitContainer;
import com.middleware.redis.unit.RedisUnitKey;

import java.util.Collection;

@FunctionalInterface
public interface RedisUnitContainerCreationFunction<R extends RedisKey, UK extends RedisUnitKey, U extends RedisUnit<UK>, V extends RedisUnitContainer<R, U>> {

    V create(R redisKey, Collection<U> units);
}
