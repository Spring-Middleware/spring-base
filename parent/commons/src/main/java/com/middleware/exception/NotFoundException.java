package com.middleware.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ServiceException {

    public NotFoundException(String errorMessage) {

        super(errorMessage, HttpStatus.NOT_FOUND);
    }

    public NotFoundException(String errorCode, String errorMessage) {

        super(errorCode, errorMessage, HttpStatus.NOT_FOUND);
    }

    public NotFoundException(String errorCode, String errorMessage, Throwable cause) {

        super(errorCode, errorMessage, HttpStatus.NOT_FOUND, cause);
    }
}
