package io.github.spring.middleware.redis.service.unit;

import io.github.spring.middleware.redis.RedisKey;
import io.github.spring.middleware.redis.component.RedisKeyMapValues;
import io.github.spring.middleware.redis.unit.RedisUnit;
import io.github.spring.middleware.redis.unit.RedisUnitKey;

import java.util.Collection;

public interface MappingAttributeFunction<K extends RedisKey, UK extends RedisUnitKey, U extends RedisUnit<UK>> {

    RedisKeyMapValues<K> map(K redisKey, Collection<U> units);

}
