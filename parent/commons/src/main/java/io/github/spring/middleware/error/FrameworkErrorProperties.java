package io.github.spring.middleware.error;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "middleware")
public class FrameworkErrorProperties {

    private Map<String, Integer> errors = new HashMap<>();

    @PostConstruct
    public void init() {
        // Set default HTTP status codes for common exceptions
        errors.putIfAbsent("PROXY_CLIENT_ERROR", 502);
        errors.putIfAbsent("PROXY_CLIENT_UNAVAILABLE_ERROR", 503);
        errors.putIfAbsent("FRAMEWORK:UNKNOWN_ERROR", 500);
        errors.putIfAbsent("FRAMEWORK:VALIDATION_ERROR", 400);
        errors.putIfAbsent("FRAMEWORK:CALL_NOT_PERMITTED", 503);
        errors.putIfAbsent("MISSING_CREDENTIALS", 401);
        errors.putIfAbsent("INVALID_CREDENTIALS", 401);
        errors.putIfAbsent("AUTHENTICATION_FAILED", 403);
    }

}