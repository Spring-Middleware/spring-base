package com.core.redis.exception;

public class JedisConnectionException extends RedisException {

    public JedisConnectionException(String message) {

        super(message);
    }

    public JedisConnectionException(String message, Exception cause) {

        super(message + ":" + cause.getMessage(), cause);
    }

}

