package io.github.spring.middleware.controller;

import io.github.spring.middleware.error.ErrorMessage;
import io.github.spring.middleware.exception.ExceptionUtils;
import io.github.spring.middleware.exception.NotFoundException;
import io.github.spring.middleware.exception.ServiceException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

@Slf4j
public abstract class CommonsController {

    @Autowired
    private ErrorMessageFactory errorMessageFactory;

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({NotFoundException.class})
    public ErrorMessage notFound(NotFoundException notFoundException) {

        return errorMessageFactory.getErrorMessage(notFoundException);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({HttpMessageNotReadableException.class, ConstraintViolationException.class, WebExchangeBindException.class, ServerWebInputException.class})
    public ErrorMessage badRequest(Exception exception) {

        return errorMessageFactory.getErrorMessage(new ServiceException(exception, HttpStatus.BAD_REQUEST));
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler({Exception.class})
    public ErrorMessage unknown(Exception exception) {

        log.error(ExceptionUtils.getStackTrace(exception, 10));
        return errorMessageFactory.getErrorMessage(new ServiceException(exception));
    }

}
