package com.core.redis.component;

import com.core.redis.RedisKey;
import com.core.redis.unit.RedisUnit;
import com.core.redis.unit.RedisUnitKey;

import java.util.Collection;

@FunctionalInterface
public interface MappingUnitFunction<K extends RedisKey, UK extends RedisUnitKey, U extends RedisUnit<UK>> {

    Collection<U> map(RedisKeyMapValues<K> redisKeyMapValues);

}
