package io.github.spring.middleware.exception;

import io.github.spring.middleware.error.ErrorDescriptor;
import org.springframework.http.HttpStatus;

public class BadRequestException extends ServiceException {

    public BadRequestException(ErrorDescriptor descriptor, String message) {
        super(HttpStatus.BAD_REQUEST, descriptor, message);
    }

    public BadRequestException(ErrorDescriptor descriptor, String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, descriptor, message, cause);
    }
}
