package com.core.redis.service;

import com.core.redis.GetFunctions;
import com.core.redis.RedisKey;
import com.core.redis.RedisKeyValue;
import com.core.redis.RedisValue;
import com.core.redis.SetFunctions;
import com.core.redis.exception.RedisException;

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
