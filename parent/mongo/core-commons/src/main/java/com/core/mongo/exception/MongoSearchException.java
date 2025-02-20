package com.core.mongo.exception;

public class MongoSearchException extends RuntimeException {

    public MongoSearchException(String message) {

        this(message, null);
    }

    public MongoSearchException(Throwable cause) {

        this(null, cause);
    }

    public MongoSearchException(String message, Throwable cause) {

        super(message, cause);
    }

}
