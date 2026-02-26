package com.middleware.redis.react.service;

import com.middleware.redis.GetFunctions;
import com.middleware.redis.RedisKey;
import com.middleware.redis.RedisKeyValue;
import com.middleware.redis.RedisValue;
import com.middleware.redis.SetFunctions;
import io.lettuce.core.RedisException;
import reactor.core.publisher.Mono;

import java.util.List;

public interface RedisReactiveService<K extends RedisKey, V extends RedisValue, KV extends RedisKeyValue<K, V>> {

    Mono<List<KV>> getKeyValues(Mono<List<K>> redisKeys, GetFunctions<K, V, KV> getFunctions);

    Mono<List<KV>> setKeyValues(Mono<List<K>> redisKeys,
                          SetFunctions<K, V, KV> setFunction) throws RedisException;

}
