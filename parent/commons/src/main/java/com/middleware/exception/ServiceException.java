package com.middleware.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class ServiceException extends RuntimeException {

    private String errorCode;
    private final HttpStatus httpStatus;

    public ServiceException(Throwable cause) {

        this(cause.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }

    public ServiceException(Throwable cause, HttpStatus httpStatus) {

        this(cause.getMessage(), httpStatus, cause);
    }

    public ServiceException() {

        this(null, HttpStatus.INTERNAL_SERVER_ERROR, null);
    }

    public ServiceException(String errorMessage) {

        this(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR, null);
    }

    public ServiceException(String errorCode, String errorMessage) {

        this(errorCode, errorMessage, HttpStatus.INTERNAL_SERVER_ERROR, null);
    }

    public ServiceException(String errorMessage, HttpStatus httpStatus, Throwable cause) {

        super(errorMessage, cause);
        this.httpStatus = httpStatus;
    }

    public ServiceException(String errorCode, String errorMessage, HttpStatus httpStatus, Throwable cause) {
        super(errorMessage, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public ServiceException(String errorMessage, Throwable cause) {

        this(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }

    public ServiceException(String errorCode, String errorMessage, Throwable cause) {

        this(errorCode, errorMessage, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }

    public ServiceException(String errorMessage, HttpStatus httpStatus) {

        this(errorMessage, httpStatus, null);
    }

    public ServiceException(String errorCode, String errorMessage, HttpStatus httpStatus) {

        this(errorCode, errorMessage, httpStatus, null);

    }

}
