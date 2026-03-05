package io.github.spring.middleware.exception;

import io.github.spring.middleware.error.ErrorDescriptor;
import org.springframework.http.HttpStatus;


public class AlreadyExistsException extends ServiceException {

    public AlreadyExistsException(ErrorDescriptor descriptor, String message) {
        super(HttpStatus.CONFLICT, descriptor, message);
    }

    public AlreadyExistsException(ErrorDescriptor descriptor, String message, Throwable cause) {
        super(HttpStatus.CONFLICT, descriptor, message, cause);
    }
}
