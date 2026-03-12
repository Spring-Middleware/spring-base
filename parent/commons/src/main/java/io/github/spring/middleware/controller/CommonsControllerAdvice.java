package io.github.spring.middleware.controller;

import io.github.spring.middleware.config.PropertyNames;
import io.github.spring.middleware.environment.Environment;
import io.github.spring.middleware.environment.EnvironmentManager;
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
    private final EnvironmentManager enviromentManage;

    public CommonsControllerAdvice(ErrorMessageFactory errorMessageFactory, EnvironmentManager enviromentManage) {
        this.errorMessageFactory = errorMessageFactory;
        this.enviromentManage = enviromentManage;
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorMessage> handle(Throwable ex, HttpServletRequest request) {
        log.error("Unhandled exception processing {} {}. requestId={}, spanId={}",
                request.getMethod(),
                request.getRequestURI(),
                MDC.get("requestId"),
                MDC.get("spanId"),
                ex);

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

        if (enviromentManage.getActiveEnvironment() != Environment.PROD) {
            ext.putIfAbsent("exception", ex.getClass().getName());
            ext.putIfAbsent("exceptionMessage", ex.getMessage());
        }

        body.setExtensions(ext);

        return ResponseEntity
                .status(body.getStatusCode())
                .body(body);
    }
}