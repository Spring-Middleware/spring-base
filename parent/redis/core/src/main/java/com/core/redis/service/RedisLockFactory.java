package com.core.redis.service;

import com.core.redis.component.RedisCommands;
import org.springframework.stereotype.Component;

@Component
public class RedisLockFactory {

    public RedisLock getRedisLock(RedisCommands redisCommands, String key) {

        return new RedisLock(redisCommands, key);
    }

}
