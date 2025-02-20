package com.core.redis.react.service;

import com.core.redis.GetFunctions;
import com.core.redis.RedisKey;
import com.core.redis.RedisKeyValue;
import com.core.redis.RedisValue;
import com.core.redis.SetFunctions;
import io.lettuce.core.RedisException;
import reactor.core.publisher.Mono;

import java.util.List;

public interface RedisReactiveService<K extends RedisKey, V extends RedisValue, KV extends RedisKeyValue<K, V>> {

    Mono<List<KV>> getKeyValues(Mono<List<K>> redisKeys, GetFunctions<K, V, KV> getFunctions);

    Mono<List<KV>> setKeyValues(Mono<List<K>> redisKeys,
                          SetFunctions<K, V, KV> setFunction) throws RedisException;

}
