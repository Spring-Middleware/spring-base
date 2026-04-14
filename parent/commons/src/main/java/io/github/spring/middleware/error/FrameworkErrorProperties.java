package io.github.spring.middleware.error;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

import static io.github.spring.middleware.error.FrameworkErrorCodes.CALL_NOT_PERMITTED;
import static io.github.spring.middleware.error.FrameworkErrorCodes.PROXY_CLIENT_ERROR;
import static io.github.spring.middleware.error.FrameworkErrorCodes.PROXY_CLIENT_UNAVAILABLE_ERROR;
import static io.github.spring.middleware.error.FrameworkErrorCodes.UNKNOWN_ERROR;
import static io.github.spring.middleware.error.FrameworkErrorCodes.VALIDATION_ERROR;
import static io.github.spring.middleware.error.SecurityErrorCodes.AUTHENTICATION_FAILED;
import static io.github.spring.middleware.error.SecurityErrorCodes.INVALID_CREDENTIALS;
import static io.github.spring.middleware.error.SecurityErrorCodes.MISSING_CREDENTIALS;

@Data
@ConfigurationProperties(prefix = "middleware")
public class FrameworkErrorProperties {

    private Map<String, Integer> errors = new HashMap<>();

    @PostConstruct
    public void init() {
        // Set default HTTP status codes for common exceptions
        errors.putIfAbsent(PROXY_CLIENT_ERROR.getCode(), 502);
        errors.putIfAbsent(PROXY_CLIENT_UNAVAILABLE_ERROR.getCode(), 503);
        errors.putIfAbsent(UNKNOWN_ERROR.getCode(), 500);
        errors.putIfAbsent(VALIDATION_ERROR.getCode(), 400);
        errors.putIfAbsent(CALL_NOT_PERMITTED.getCode(), 503);
        errors.putIfAbsent(MISSING_CREDENTIALS.getCode(), 401);
        errors.putIfAbsent(INVALID_CREDENTIALS.getCode(), 401);
        errors.putIfAbsent(AUTHENTICATION_FAILED.getCode(), 403);
    }

}