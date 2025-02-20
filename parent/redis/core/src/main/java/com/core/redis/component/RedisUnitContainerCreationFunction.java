package com.core.redis.component;

import com.core.redis.RedisKey;
import com.core.redis.unit.RedisUnit;
import com.core.redis.unit.RedisUnitContainer;
import com.core.redis.unit.RedisUnitKey;

import java.util.Collection;

@FunctionalInterface
public interface RedisUnitContainerCreationFunction<R extends RedisKey, UK extends RedisUnitKey, U extends RedisUnit<UK>, V extends RedisUnitContainer<R, U>> {

    V create(R redisKey, Collection<U> units);
}
