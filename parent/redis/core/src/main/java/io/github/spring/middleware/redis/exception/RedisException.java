package io.github.spring.middleware.redis.exception;

public class RedisException extends RuntimeException {

    public RedisException(String message) {

        super(message);
    }

    public RedisException(String message, Exception cause) {

        super(message + ":" + cause.getMessage(), cause);
    }

}
