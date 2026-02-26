package com.middleware.redis.service.unit;

import com.middleware.redis.component.MappingUnitFunction;
import com.middleware.redis.RedisKey;
import com.middleware.redis.component.RedisUnitContainerCreationFunction;
import com.middleware.redis.unit.RedisUnit;
import com.middleware.redis.unit.RedisUnitContainer;
import com.middleware.redis.unit.RedisUnitKey;

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
