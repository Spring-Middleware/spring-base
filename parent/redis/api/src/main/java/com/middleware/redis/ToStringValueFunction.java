package com.middleware.redis;

@FunctionalInterface
public interface ToStringValueFunction<V extends RedisValue> {

    String apply(V value);

}
