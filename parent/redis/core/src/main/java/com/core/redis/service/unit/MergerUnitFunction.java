package com.core.redis.service.unit;

import com.core.redis.unit.RedisUnit;
import com.core.redis.unit.RedisUnitKey;

import java.util.Collection;

@FunctionalInterface
public interface MergerUnitFunction<K extends RedisUnitKey, U extends RedisUnit<K>> {

    Collection<U> merge(Collection<U> currentUnits, Collection<U> nextUnits);
}
