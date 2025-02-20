package com.core.redis.service.unit;

import com.core.redis.component.MappingUnitFunction;
import com.core.redis.RedisKey;
import com.core.redis.component.RedisUnitContainerCreationFunction;
import com.core.redis.unit.RedisUnit;
import com.core.redis.unit.RedisUnitContainer;
import com.core.redis.unit.RedisUnitKey;

import java.util.Collection;
import java.util.function.Predicate;

public interface RedisUnitService<R extends RedisKey, UK extends RedisUnitKey, U extends RedisUnit<UK>, V extends RedisUnitContainer<R, U>> {

    Collection<V> getRedisUnitContainers(Collection<R> keys, MappingUnitFunction<R, UK, U> mappingFunction,
                                         RedisUnitContainerCreationFunction<R, UK, U, V> creationFunction,
                                         Predicate<U> filterForUnits);

    Collection<V> setRedisUnitContainers(Collection<V> unitContainers, MappingUnitFunction<R, UK, U> mappingFunction,
                                         MappingAttributeFunction mappingAttributeFunction,
                                         MergerUnitFunction<UK, U> mergerUnitFunction,
                                         RedisUnitContainerCreationFunction<R, UK, U, V> creationFunction);

}
