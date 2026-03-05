package io.github.spring.middleware.controller;

import io.github.spring.middleware.config.PropertyNames;
import io.github.spring.middleware.error.ErrorMessage;
import io.github.spring.middleware.error.ErrorMessageFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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

        // Enrich extensions (safe)
        Map<String, Object> ext = new HashMap<>();
        if (body.getExtensions() != null) ext.putAll(body.getExtensions());

        ext.putIfAbsent("http.path", request.getRequestURI());
        ext.putIfAbsent("http.method", request.getMethod());

        // requestId from request attribute (si lo estás propagando así)
        Object reqId = request.getAttribute(PropertyNames.REQUEST_ID);
        if (reqId != null) ext.putIfAbsent("requestId", reqId.toString());

        // traceId from MDC (si lo usas)
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) ext.putIfAbsent("traceId", traceId);

        // Si tu ErrorMessage permite setExtensions:
        body.setExtensions(ext);

        return ResponseEntity
                .status(body.getStatusCode())
                .body(body);
    }
}
