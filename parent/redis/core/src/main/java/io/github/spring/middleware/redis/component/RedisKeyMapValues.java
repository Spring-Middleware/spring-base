package io.github.spring.middleware.redis.component;

import io.github.spring.middleware.redis.RedisKey;

import java.util.Map;

public class RedisKeyMapValues<K extends RedisKey> {

    private final K redisKey;
    private final Map<String, String> values;

    public RedisKeyMapValues(K redisKey, Map<String, String> values) {

        this.redisKey = redisKey;
        this.values = values;
    }

    public K getRedisKey() {

        return redisKey;
    }

    public Map<String, String> getValues() {

        return values;
    }
}
