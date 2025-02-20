package com.core.redis.react.service;

import com.core.redis.GetFunctions;
import com.core.redis.RedisKey;
import com.core.redis.RedisKeyValue;
import com.core.redis.RedisValue;
import com.core.redis.SetFunctions;
import com.core.redis.ToKeyValueFunction;
import com.core.redis.ToValueFunction;
import com.core.redis.react.component.RedisReactiveOperationManager;
import io.lettuce.core.RedisException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RedisReactiveServiceImpl<K extends RedisKey, V extends RedisValue, KV extends RedisKeyValue<K, V>> implements RedisReactiveService<K, V, KV> {

    @Autowired
    private RedisReactiveOperationManager<K> redisReactiveOperationManager;

    @Override
    public Mono<List<KV>> getKeyValues(Mono<List<K>> redisKeys,
                                       GetFunctions<K, V, KV> getFunctions) throws RedisException {

        return redisReactiveOperationManager.readValues(redisKeys).map(pair -> {
            List<String> values = pair.getValue().block();
            List<KV> keysValue = toKeyValues(values, pair.getKey(), getFunctions.toValueFunction(),
                    getFunctions.toKeyValueFunction());
            return keysValue;
        });
    }

    private List<KV> toKeyValues(List<String> values, List<K> keys, ToValueFunction<K, V> toValueFunction,
                                 ToKeyValueFunction<K, V, KV> toKeyValueFunction) {

        return IntStream.range(0, keys.size()).mapToObj(i -> {
            K currentKey = keys.get(i);
            V currentValue = toValueFunction.apply(currentKey, values.get(i));
            return toKeyValueFunction.apply(currentKey, currentValue);
        }).collect(Collectors.toList());
    }

    @Override
    public Mono<List<KV>> setKeyValues(Mono<List<K>> redisKeys,
                                       SetFunctions<K, V, KV> setFunction) throws RedisException {

        Mono<List<Pair<K, V>>> pairsKV = (Mono) redisReactiveOperationManager.readValues(redisKeys).map(pair -> {
            List<String> values = pair.getValue().block();
            return IntStream.range(0, pair.getKey().size()).mapToObj(i -> {
                K currentKey = pair.getKey().get(i);
                String value = values.get(i);
                V currentValue = null;
                if (value == null) {
                    currentValue = setFunction.newValueFunction().apply(currentKey);
                } else {
                    currentValue = setFunction.toValueFunction().apply(currentKey, value);
                }
                currentValue = setFunction.updateValueFunction().apply(currentKey, currentValue);
                return new ImmutablePair(currentKey, currentValue);
            }).collect(Collectors.toList());
        });
        Mono<List<Pair<String, String>>> pairs = (Mono) pairsKV
                .map(l -> l.stream().map(p -> new ImmutablePair(p.getKey().getKey(),
                        setFunction.toStringValueFunction().apply(p.getValue()))).collect(Collectors.toList()));
        redisReactiveOperationManager.writeValues(pairs);
        return pairsKV.map(l -> l.stream().map(p -> {
            return setFunction.toKeyValueFunction().apply(p.getKey(), p.getValue());
        }).collect(Collectors.toList()));
    }

}
