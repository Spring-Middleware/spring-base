package com.core.redis;

public interface SetFunctions<K extends RedisKey, V extends RedisValue, KV extends RedisKeyValue<K, V>> {

    ToStringValueFunction<V> toStringValueFunction();

    ToValueFunction<K, V> toValueFunction();

    UpdateValueFunction<K, V> updateValueFunction();

    NewValueFunction<K, V> newValueFunction();

    ToKeyValueFunction<K, V, KV> toKeyValueFunction();


}
