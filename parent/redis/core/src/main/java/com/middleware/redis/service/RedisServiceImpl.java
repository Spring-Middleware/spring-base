package com.middleware.redis.service;

import com.middleware.redis.GetFunctions;
import com.middleware.redis.RedisKey;
import com.middleware.redis.RedisKeyValue;
import com.middleware.redis.RedisValue;
import com.middleware.redis.SetFunctions;
import com.middleware.redis.component.RedisCommands;
import com.middleware.redis.component.RedisConnectionManager;
import com.middleware.redis.component.RedisOperationManager;
import com.middleware.redis.exception.RedisException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
public class RedisServiceImpl<K extends RedisKey, V extends RedisValue, KV extends RedisKeyValue<K, V>> implements RedisService<K, V, KV> {

    @Autowired
    private RedisOperationManager redisOperationManager;
    @Autowired
    private RedisConnectionManager redisConnectionManager;
    @Autowired
    private RedisLockFactory redisLockFactory;
    private RedisMutex<K> redisMutex = new RedisMutex<>();

    public Collection<KV> getKeyValues(Collection<K> redisKeys,
                                       GetFunctions<K, V, KV> getFunctions) throws RedisException {

        RedisCommands redisCommands = redisConnectionManager.getConnection();
        try {
            List<String> keysToRead = redisKeys.parallelStream().map(r -> r.getKey()).collect(Collectors.toList());
            List<String> valuesReaded = redisOperationManager.readValues(redisCommands, keysToRead);
            List<K> redisKeysList = new ArrayList(redisKeys);
            return IntStream.range(0, keysToRead.size()).mapToObj(i -> {
                V value = getFunctions.toValueFunction().apply(redisKeysList.get(i), valuesReaded.get(i));
                return getFunctions.toKeyValueFunction().apply(redisKeysList.get(i), value);
            }).collect(Collectors.toList());
        } finally {
            redisConnectionManager.closeConnection(redisCommands);
        }
    }

    public Collection<KV> setKeyValues(Collection<K> redisKeys,
                                       SetFunctions<K, V, KV> setFunctions) throws RedisException {

        RedisCommands redisCommands = redisConnectionManager.getConnection();
        try {
            List<String> keys = redisKeys.parallelStream().map(K::getKey).collect(Collectors.toList());
            List<String> values = redisOperationManager.readValues(redisCommands, keys);
            List<K> redisKeysList = new ArrayList(redisKeys);
            List<Pair<K, V>> pairsKV = (List) IntStream.range(0, redisKeys.size()).mapToObj(i -> {
                String value = values.get(i);
                K currentKey = redisKeysList.get(i);
                V currentValue = null;
                if (value == null) {
                    currentValue = setFunctions.newValueFunction().apply(currentKey);
                } else {
                    currentValue = setFunctions.toValueFunction().apply(currentKey, value);
                }
                currentValue = setFunctions.updateValueFunction().apply(currentKey, currentValue);
                return new ImmutablePair(currentKey, currentValue);
            }).collect(Collectors.toList());

            List<Pair<String, String>> pairs = pairsKV.stream()
                    .map(p -> new ImmutablePair<>(p.getKey().getKey(), setFunctions.toStringValueFunction().apply(
                            p.getValue()))).collect(Collectors.toList());
            redisOperationManager.writeValues(redisCommands, pairs);
            return pairsKV.stream().map(p -> setFunctions.toKeyValueFunction().apply(p.getKey(), p.getValue())).collect(
                    Collectors.toList());
        } finally {
            redisConnectionManager.closeConnection(redisCommands);
        }
    }

    public KV setKeyValue(K redisKey, SetFunctions<K, V, KV> setFunctions) {

        RedisCommands redisCommands = redisConnectionManager.getConnection();
        RedisLock redisLock = redisLockFactory.getRedisLock(redisCommands, "Lock-" + redisKey.getKey());
        KV redisKeyValue = null;
        try {
            redisLock.acquire();
            String key = redisKey.getKey();
            String value = redisOperationManager.readValue(redisCommands, key);
            V currentValue = null;
            if (value == null) {
                currentValue = setFunctions.newValueFunction().apply(redisKey);
            } else {
                currentValue = setFunctions.toValueFunction().apply(redisKey, value);
            }
            currentValue = setFunctions.updateValueFunction().apply(redisKey, currentValue);
            redisOperationManager
                    .writeValue(redisCommands, redisKey.getKey(),
                            setFunctions.toStringValueFunction().apply(currentValue));
            redisKeyValue = setFunctions.toKeyValueFunction().apply(redisKey, currentValue);
        } catch (Exception ex) {
            log.error("Can't write " + redisKey.getKey(), ex);
            throw new RedisException("Can't write " + redisKey.getKey(), ex);
        } finally {
            redisLock.release();
            redisConnectionManager.closeConnection(redisCommands);
        }
        redisMutex.removeMutex(redisKey);
        return redisKeyValue;
    }

    public Collection<KV> deleteKeyValues(Collection<K> redisKeys,
                                          GetFunctions<K, V, KV> getFunctions) throws RedisException {

        RedisCommands redisCommands = redisConnectionManager.getConnection();
        List<String> keys = redisKeys.stream().map(K::getKey).collect(Collectors.toList());
        Collection<KV> keyValues = getKeyValues(redisKeys, getFunctions);
        redisOperationManager.delete(redisCommands, keys.toArray(new String[keys.size()]));
        return keyValues;
    }

    public KV deleteKeyValue(K redisKey, GetFunctions<K, V, KV> getFunctions) {

        RedisCommands redisCommands = redisConnectionManager.getConnection();
        KV redisKeyValue = null;
        try {
            String jsonValue = redisOperationManager.readValue(redisCommands, redisKey.getKey());
            if (jsonValue != null) {
                V redisValue = getFunctions.toValueFunction().apply(redisKey, jsonValue);
                redisOperationManager.delete(redisCommands, redisKey.getKey());
                redisKeyValue = getFunctions.toKeyValueFunction().apply(redisKey, redisValue);
            }
        } catch (Exception ex) {
            log.error("Can't delete " + redisKey.getKey(), ex);
            throw new RedisException("Can't delete " + redisKey.getKey(), ex);
        } finally {
            redisConnectionManager.closeConnection(redisCommands);
        }
        return redisKeyValue;
    }

}
