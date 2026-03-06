package io.github.spring.middleware.controller;

import io.github.spring.middleware.config.PropertyNames;
import io.github.spring.middleware.error.ErrorMessage;
import io.github.spring.middleware.error.ErrorMessageFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class CommonsControllerAdvice {

    private final ErrorMessageFactory errorMessageFactory;

    public CommonsControllerAdvice(ErrorMessageFactory errorMessageFactory) {
        this.errorMessageFactory = errorMessageFactory;
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorMessage> handle(Throwable ex, HttpServletRequest request) {

        ErrorMessage body = errorMessageFactory.from(ex);

        Map<String, Object> ext = new HashMap<>();
        if (body.getExtensions() != null) {
            ext.putAll(body.getExtensions());
        }

        ext.putIfAbsent("http.path", request.getRequestURI());
        ext.putIfAbsent("http.method", request.getMethod());

        Object requestId = request.getAttribute(PropertyNames.REQUEST_ID);
        if (requestId == null) {
            requestId = MDC.get(PropertyNames.REQUEST_ID);
        }
        if (requestId != null && StringUtils.isNotBlank(requestId.toString())) {
            ext.putIfAbsent("requestId", requestId.toString());
        }

        Object spanId = request.getAttribute(PropertyNames.SPAN_ID);
        if (spanId == null) {
            spanId = MDC.get(PropertyNames.SPAN_ID);
        }
        if (spanId != null && StringUtils.isNotBlank(spanId.toString())) {
            ext.putIfAbsent("spanId", spanId.toString());
        }

        body.setExtensions(ext);

        return ResponseEntity
                .status(body.getStatusCode())
                .body(body);
    }
}