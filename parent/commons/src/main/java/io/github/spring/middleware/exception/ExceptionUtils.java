package io.github.spring.middleware.exception;

import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

public class ExceptionUtils {

    private ExceptionUtils() {

    }

    public static String getNotNullMessage(Throwable cause) {

        if (cause != null) {
            return Optional.ofNullable(cause.getMessage())
                    .orElseGet(() -> Optional.ofNullable(getNotNullMessage(cause.getCause()))
                            .orElseGet(() -> getStackTrace(cause, 5)));
        } else {
            return null;
        }
    }

    public static String getStackTrace(Throwable ex, int lines) {

        StackTraceElement[] stackTrace = ex.getStackTrace();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < lines; i++) {
            if (stackTrace.length > i) {
                buffer.append(stackTrace[i].toString()).append("\n");
            }
        }
        return buffer.toString();
    }

}
