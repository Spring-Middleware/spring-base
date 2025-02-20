package com.core.redis.service.unit;

import com.core.redis.RedisKey;
import com.core.redis.component.RedisKeyMapValues;
import com.core.redis.unit.RedisUnit;
import com.core.redis.unit.RedisUnitKey;

import java.util.Collection;

public interface MappingAttributeFunction<K extends RedisKey, UK extends RedisUnitKey, U extends RedisUnit<UK>> {

    RedisKeyMapValues<K> map(K redisKey, Collection<U> units);

}
