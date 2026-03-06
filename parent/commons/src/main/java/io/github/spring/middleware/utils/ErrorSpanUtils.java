package io.github.spring.middleware.utils;

import io.github.spring.middleware.error.ErrorMessage;
import io.github.spring.middleware.error.ErrorSpanStep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ErrorSpanUtils {

    private static final String SPAN = "span";

    private ErrorSpanUtils() {
    }

    public static void ensureMutableExtensions(ErrorMessage errorMessage) {
        if (errorMessage.getExtensions() == null) {
            errorMessage.setExtensions(new HashMap<>());
        } else if (!(errorMessage.getExtensions() instanceof HashMap)) {
            errorMessage.setExtensions(new HashMap<>(errorMessage.getExtensions()));
        }
    }

    @SuppressWarnings("unchecked")
    public static void appendTrace(ErrorMessage errorMessage, ErrorSpanStep step) {
        ensureMutableExtensions(errorMessage);

        Object spanObj = errorMessage.getExtensions().get(SPAN);

        List<Map<String, Object>> span;
        if (spanObj instanceof List<?> existingList) {
            span = (List<Map<String, Object>>) existingList;
        } else {
            span = new ArrayList<>();
            errorMessage.getExtensions().put(SPAN, span);
        }

        Map<String, Object> traceEntry = new HashMap<>();
        traceEntry.put("service", step.getService());
        traceEntry.put("method", step.getMethod());
        traceEntry.put("url", step.getUrl());
        traceEntry.put("httpStatus", step.getHttpStatus());
        traceEntry.put("requestId", step.getRequestId());

        span.add(traceEntry);
    }

}
