package io.github.spring.middleware.exception;

import io.github.spring.middleware.error.ErrorDescriptor;
import org.springframework.http.HttpStatus;


public class ConflictException extends ServiceException {

    public ConflictException(ErrorDescriptor descriptor, String message) {
        super(HttpStatus.CONFLICT, descriptor, message);
    }

    public ConflictException(ErrorDescriptor descriptor, String message, Throwable cause) {
        super(HttpStatus.CONFLICT, descriptor, message, cause);
    }
}
