package com.middleware.redis.service;

import com.middleware.redis.component.RedisCommands;
import org.springframework.stereotype.Component;

@Component
public class RedisLockFactory {

    public RedisLock getRedisLock(RedisCommands redisCommands, String key) {

        return new RedisLock(redisCommands, key);
    }

}
