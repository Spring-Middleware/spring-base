package io.github.spring.middleware.error;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "spring-middleware.errors")
public class FrameworkErrorProperties {

    private Map<String, Integer> httpStatus = new HashMap<>();

    @PostConstruct
    public void init() {
        // Set default HTTP status codes for common exceptions
        httpStatus.putIfAbsent("PROXY_CLIENT_ERROR", 502);
        httpStatus.putIfAbsent("FRAMEWORK:UNKNOWN_ERROR", 500);
    }

    public Map<String, Integer> getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Map<String, Integer> httpStatus) {
        this.httpStatus = httpStatus;
    }
}