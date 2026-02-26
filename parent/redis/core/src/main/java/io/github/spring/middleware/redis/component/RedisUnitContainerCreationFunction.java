package io.github.spring.middleware.redis.component;

import io.github.spring.middleware.redis.RedisKey;
import io.github.spring.middleware.redis.unit.RedisUnit;
import io.github.spring.middleware.redis.unit.RedisUnitContainer;
import io.github.spring.middleware.redis.unit.RedisUnitKey;

import java.util.Collection;

@FunctionalInterface
public interface RedisUnitContainerCreationFunction<R extends RedisKey, UK extends RedisUnitKey, U extends RedisUnit<UK>, V extends RedisUnitContainer<R, U>> {

    V create(R redisKey, Collection<U> units);
}
