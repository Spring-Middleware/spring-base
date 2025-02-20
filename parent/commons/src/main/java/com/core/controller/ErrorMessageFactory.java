package com.core.controller;

import com.core.error.ErrorMessage;
import com.core.exception.ExceptionUtils;
import com.core.exception.ServiceException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

@Component
public class ErrorMessageFactory<E extends ServiceException> {

    public ErrorMessage getErrorMessage(E serviceException) {

        return ErrorMessage.builder()
                .statusCode(serviceException.getHttpStatus().value())
                .statusMessage(serviceException.getHttpStatus().getReasonPhrase())
                .errorCode(serviceException.getErrorCode())
                .errorMessage(
                        Optional.ofNullable(serviceException.getMessage()).orElseGet(
                                () -> Optional.ofNullable(serviceException.getCause())
                                        .map(ex -> ExceptionUtils.getNotNullMessage(ex)).orElse(
                                                StringUtils.EMPTY))).build();

    }

    public ErrorMessage getErrorMessageFromThrowable(Throwable ex) {

        ErrorMessage errorMessage = null;
        if (ex instanceof WebClientResponseException) {
            WebClientResponseException webClientResponseException = (WebClientResponseException) ex;
            try {
                errorMessage = webClientResponseException.getResponseBodyAs(ErrorMessage.class);
            } catch (Exception wex) {
                errorMessage = getErrorMessage((E) new ServiceException(wex));
            }
        } else {
            errorMessage = getErrorMessage((E) new ServiceException(ex));
        }
        return errorMessage;
    }

}
