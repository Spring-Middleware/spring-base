package io.github.spring.middleware.redis.service;

import io.github.spring.middleware.redis.component.RedisCommands;
import org.springframework.stereotype.Component;

@Component
public class RedisLockFactory {

    public RedisLock getRedisLock(RedisCommands redisCommands, String key) {

        return new RedisLock(redisCommands, key);
    }

}
