package io.github.spring.middleware.redis.service;

import io.github.spring.middleware.redis.GetFunctions;
import io.github.spring.middleware.redis.RedisKey;
import io.github.spring.middleware.redis.RedisKeyValue;
import io.github.spring.middleware.redis.RedisValue;
import io.github.spring.middleware.redis.SetFunctions;
import io.github.spring.middleware.redis.exception.RedisException;

import java.util.Collection;

public interface RedisService<K extends RedisKey, V extends RedisValue, KV extends RedisKeyValue<K, V>> {

    Collection<KV> getKeyValues(Collection<K> redisKeys,
                                GetFunctions<K, V, KV> getFunctions) throws RedisException;

    Collection<KV> setKeyValues(Collection<K> redisKeys,
                          SetFunctions<K, V, KV> setFunction) throws RedisException;

    KV setKeyValue(K redisKey, SetFunctions<K, V, KV> setFunctions);

    Collection<KV> deleteKeyValues(Collection<K> redisKeys, GetFunctions<K, V, KV> getFunctions) throws RedisException;

    KV deleteKeyValue(K redisKey, GetFunctions<K, V, KV> getFunctions);

}
