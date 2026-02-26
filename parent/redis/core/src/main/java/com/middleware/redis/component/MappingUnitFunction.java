package com.middleware.redis.component;

import com.middleware.redis.RedisKey;
import com.middleware.redis.unit.RedisUnit;
import com.middleware.redis.unit.RedisUnitKey;

import java.util.Collection;

@FunctionalInterface
public interface MappingUnitFunction<K extends RedisKey, UK extends RedisUnitKey, U extends RedisUnit<UK>> {

    Collection<U> map(RedisKeyMapValues<K> redisKeyMapValues);

}
