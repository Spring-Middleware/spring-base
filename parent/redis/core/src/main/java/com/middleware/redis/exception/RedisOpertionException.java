package com.middleware.redis.exception;

public class RedisOpertionException extends RedisException {

    public RedisOpertionException(String message) {

        super(message);
    }

    public RedisOpertionException(String message, Exception cause) {

        super(message + ":" + cause.getMessage(), cause);
    }

}
