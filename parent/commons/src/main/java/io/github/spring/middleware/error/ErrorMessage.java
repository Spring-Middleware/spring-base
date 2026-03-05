package io.github.spring.middleware.error;

import java.util.HashMap;
import java.util.Map;

public class ErrorMessage implements ErrorDescriptor {

    private int statusCode;
    private String statusMessage;
    private String errorCode;
    private String errorMessage;
    private Map<String, Object> extensions = new HashMap<>();

    public ErrorMessage() {
    }

    public ErrorMessage(int statusCode,
                        String statusMessage,
                        String errorCode,
                        String errorMessage,
                        Map<String, Object> extensions) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.extensions = extensions;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getCode() {
        return errorCode;
    }

    public String getMessage() {
        return errorMessage;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }
}
