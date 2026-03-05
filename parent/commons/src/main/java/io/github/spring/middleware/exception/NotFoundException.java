package io.github.spring.middleware.exception;

import io.github.spring.middleware.error.ErrorDescriptor;
import org.springframework.http.HttpStatus;

public class NotFoundException extends ServiceException {

    public NotFoundException(ErrorDescriptor descriptor, String message) {
        super(HttpStatus.NOT_FOUND, descriptor, message);
    }

    public NotFoundException(ErrorDescriptor descriptor, String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, descriptor, message, cause);
    }
}
