package io.github.spring.middleware.redis.react.service;

import io.github.spring.middleware.redis.GetFunctions;
import io.github.spring.middleware.redis.RedisKey;
import io.github.spring.middleware.redis.RedisKeyValue;
import io.github.spring.middleware.redis.RedisValue;
import io.github.spring.middleware.redis.SetFunctions;
import io.lettuce.core.RedisException;
import reactor.core.publisher.Mono;

import java.util.List;

public interface RedisReactiveService<K extends RedisKey, V extends RedisValue, KV extends RedisKeyValue<K, V>> {

    Mono<List<KV>> getKeyValues(Mono<List<K>> redisKeys, GetFunctions<K, V, KV> getFunctions);

    Mono<List<KV>> setKeyValues(Mono<List<K>> redisKeys,
                          SetFunctions<K, V, KV> setFunction) throws RedisException;

}
