package com.middleware.redis.service.unit;

import com.middleware.redis.unit.RedisUnit;
import com.middleware.redis.unit.RedisUnitKey;

import java.util.Collection;

@FunctionalInterface
public interface MergerUnitFunction<K extends RedisUnitKey, U extends RedisUnit<K>> {

    Collection<U> merge(Collection<U> currentUnits, Collection<U> nextUnits);
}
