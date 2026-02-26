package io.github.spring.middleware.redis.service.unit;

import io.github.spring.middleware.redis.component.MappingUnitFunction;
import io.github.spring.middleware.redis.RedisKey;
import io.github.spring.middleware.redis.component.RedisUnitContainerCreationFunction;
import io.github.spring.middleware.redis.unit.RedisUnit;
import io.github.spring.middleware.redis.unit.RedisUnitContainer;
import io.github.spring.middleware.redis.unit.RedisUnitKey;

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
