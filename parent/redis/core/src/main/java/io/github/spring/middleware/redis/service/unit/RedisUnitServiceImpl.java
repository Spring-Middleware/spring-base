package io.github.spring.middleware.redis.service.unit;

import io.github.spring.middleware.redis.component.MappingUnitFunction;
import io.github.spring.middleware.redis.RedisKey;
import io.github.spring.middleware.redis.component.RedisKeyMapValues;
import io.github.spring.middleware.redis.component.RedisUnitContainerCreationFunction;
import io.github.spring.middleware.redis.component.RedisCommands;
import io.github.spring.middleware.redis.component.RedisConnectionManager;
import io.github.spring.middleware.redis.component.RedisOperationManager;
import io.github.spring.middleware.redis.exception.RedisException;
import io.github.spring.middleware.redis.service.RedisLock;
import io.github.spring.middleware.redis.service.RedisLockFactory;
import io.github.spring.middleware.redis.service.RedisMutex;
import io.github.spring.middleware.redis.unit.RedisUnit;
import io.github.spring.middleware.redis.unit.RedisUnitContainer;
import io.github.spring.middleware.redis.unit.RedisUnitKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RedisUnitServiceImpl<R extends RedisKey, UK extends RedisUnitKey, U extends RedisUnit<UK>, V extends RedisUnitContainer<R, U>> implements RedisUnitService<R, UK, U, V> {

    @Autowired
    private RedisOperationManager redisOperationManager;
    @Autowired
    private RedisConnectionManager redisConnectionManager;
    @Autowired
    private RedisLockFactory redisLockFactory;
    private RedisMutex<V> redisMutex = new RedisMutex<>();

    @Override
    public Collection<V> getRedisUnitContainers(Collection<R> redisKeys, MappingUnitFunction<R, UK, U> mappingFunction,
                                                RedisUnitContainerCreationFunction<R, UK, U, V> creationFunction,
                                                Predicate<U> filterForUnits) {

        return redisKeys.parallelStream().map(redisKey -> {
            return getRedisUnitContainer(redisKey, mappingFunction, creationFunction, filterForUnits);
        }).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private V getRedisUnitContainer(R redisKey, MappingUnitFunction<R, UK, U> mappingFunction,
                                    RedisUnitContainerCreationFunction<R, UK, U, V> creationFunction,
                                    Predicate<U> filterForUnits) {

        RedisCommands redisCommands = redisConnectionManager.getConnection();
        try {
            Collection<U> units = getRedisUnits(redisCommands, redisKey, mappingFunction, filterForUnits);
            return creationFunction.create(redisKey, units);
        } finally {
            redisConnectionManager.closeConnection(redisCommands);
        }

    }

    private Collection<U> getRedisUnits(RedisCommands redisCommands, R redisKey,
                                        MappingUnitFunction<R, UK, U> mappingFunction,
                                        Predicate<U> filterForUnits) {

        Map<String, String> map = redisOperationManager.read(redisCommands, redisKey.getKey());
        Collection<U> units = (Collection) Optional.ofNullable(map)
                .map(m -> mappingFunction.map(new RedisKeyMapValues(redisKey, m)))
                .orElse(null);
        if (filterForUnits != null && units != null) {
            units = units.stream().filter(filterForUnits).collect(Collectors.toSet());
        }
        return units;
    }

    public Collection<V> setRedisUnitContainers(Collection<V> unitContainers,
                                                MappingUnitFunction<R, UK, U> mappingFunction,
                                                MappingAttributeFunction mappingAttributeFunction,
                                                MergerUnitFunction<UK, U> mergerUnitFunction,
                                                RedisUnitContainerCreationFunction<R, UK, U, V> creationFunction) {

        return unitContainers.stream().map(unitContainer -> {
            RedisCommands redisCommands = redisConnectionManager.getConnection();
            try {
                return setUnitContainer(redisCommands, unitContainer, mappingFunction,
                        mappingAttributeFunction, mergerUnitFunction, creationFunction);
            } finally {
                redisConnectionManager.closeConnection(redisCommands);
            }
        }).collect(Collectors.toSet());

    }

    private V setUnitContainer(RedisCommands jedis, V unitContainer,
                               MappingUnitFunction<R, UK, U> mappingFunction,
                               MappingAttributeFunction mappingAttributeFunction,
                               MergerUnitFunction<UK, U> mergerUnitFunction,
                               RedisUnitContainerCreationFunction<R, UK, U, V> creationFunction)

            throws RedisException {

        synchronized (redisMutex.getMutex(unitContainer)) {
            RedisCommands redisCommands = redisConnectionManager.getConnection();
            RedisLock redisLock = redisLockFactory
                    .getRedisLock(redisCommands, "Lock-" + unitContainer.getRedisKey().getKey());
            Collection<U> mergedUnits = null;
            try {
                redisLock.acquire();
                Collection<U> currentUnits = getRedisUnits(redisCommands, unitContainer.getRedisKey(), mappingFunction,
                        null);
                mergedUnits = mergerUnitFunction.merge(currentUnits, unitContainer.getRedisUnits());
                if (!CollectionUtils.emptyIfNull(mergedUnits).isEmpty()) {
                    RedisKeyMapValues redisKeyMapValues = mappingAttributeFunction
                            .map(unitContainer.getRedisKey(), mergedUnits);
                    redisOperationManager.write(redisCommands, redisKeyMapValues.getRedisKey().getKey(),
                            redisKeyMapValues.getValues());
                }

            } catch (RedisException rex) {
                throw rex;
            } catch (Exception ex) {
                log.error("Can't wrtie " + unitContainer.getRedisKey().getKey(), ex);
                throw new RedisException("Can't wrtie " + unitContainer.getRedisKey().getKey(), ex);
            } finally {
                redisLock.release();
            }
            redisMutex.removeMutex(unitContainer);
            return creationFunction.create(unitContainer.getRedisKey(), mergedUnits);
        }
    }
}
