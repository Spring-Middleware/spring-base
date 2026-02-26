package com.middleware.redis.service.unit;

import com.middleware.redis.RedisKey;
import com.middleware.redis.component.RedisKeyMapValues;
import com.middleware.redis.unit.RedisUnit;
import com.middleware.redis.unit.RedisUnitKey;

import java.util.Collection;

public interface MappingAttributeFunction<K extends RedisKey, UK extends RedisUnitKey, U extends RedisUnit<UK>> {

    RedisKeyMapValues<K> map(K redisKey, Collection<U> units);

}
