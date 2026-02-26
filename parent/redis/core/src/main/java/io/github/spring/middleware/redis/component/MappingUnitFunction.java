package io.github.spring.middleware.redis.component;

import io.github.spring.middleware.redis.RedisKey;
import io.github.spring.middleware.redis.unit.RedisUnit;
import io.github.spring.middleware.redis.unit.RedisUnitKey;

import java.util.Collection;

@FunctionalInterface
public interface MappingUnitFunction<K extends RedisKey, UK extends RedisUnitKey, U extends RedisUnit<UK>> {

    Collection<U> map(RedisKeyMapValues<K> redisKeyMapValues);

}
