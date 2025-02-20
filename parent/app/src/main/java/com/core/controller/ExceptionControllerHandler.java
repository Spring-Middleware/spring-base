package com.core.controller;

import com.core.exception.BadRequestException;
import com.core.exception.ExceptionUtils;
import com.core.exception.ServiceException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;



@ControllerAdvice
public class ExceptionControllerHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<String> handleConstraintException(ConstraintViolationException constraintViolationException) {

        return new ResponseEntity<>(constraintViolationException.getConstraintViolations()
                .stream().map(v -> v.getPropertyPath().toString() + ":  " + v.getMessage())
                .reduce((s1, s2) -> s1 + "\n" + s2).orElse(null), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({BadRequestException.class})
    public ResponseEntity<String> handleBadRequestException(BadRequestException badRequestException) {

        return new ResponseEntity<>(ExceptionUtils.getNotNullMessage(badRequestException), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<String> handleException(Exception unknownException) {

        return new ResponseEntity<>(ExceptionUtils.getNotNullMessage(unknownException),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({ServiceException.class})
    public ResponseEntity<String> handlerServiceException(ServiceException serviceException) {

        return new ResponseEntity<>(ExceptionUtils.getNotNullMessage(serviceException),
                serviceException.getHttpStatus());
    }

}
