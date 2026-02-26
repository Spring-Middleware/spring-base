package com.middleware.redis;

public interface GetFunctions<K extends RedisKey, V extends RedisValue, KV extends RedisKeyValue<K, V>> {

    ToKeyValueFunction<K, V, KV> toKeyValueFunction();

    ToValueFunction<K, V> toValueFunction();


}
