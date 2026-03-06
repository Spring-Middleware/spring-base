package io.github.spring.middleware.client.proxy;

import io.github.spring.middleware.error.ErrorMessage;
import io.github.spring.middleware.error.RemoteError;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RemoteServerException extends RuntimeException implements RemoteError {

    private final int httpStatusCode;
    private final ErrorMessage errorMessage;
    private final Map<String, Object> extensions = new HashMap<>();

    public RemoteServerException(ErrorMessage errorMessage, int httpStatusCode, String requestId) {
        super(errorMessage != null ? errorMessage.getMessage() : null);
        this.httpStatusCode = httpStatusCode;
        this.errorMessage = errorMessage;

        if (StringUtils.isNotBlank(requestId)) {
            this.extensions.put("requestId", requestId);
        }
    }

    public RemoteServerException(String message, Exception e, int httpStatusCode, String requestId) {
        super(message, e);
        this.httpStatusCode = httpStatusCode;
        this.errorMessage = null;

        if (StringUtils.isNotBlank(requestId)) {
            this.extensions.put("requestId", requestId);
        }
    }

    public String getRequestId() {
        return Optional.ofNullable((String) this.extensions.get("requestId"))
                .orElse(StringUtils.EMPTY);
    }

    @Override
    public Object getPayload() {
        return errorMessage;
    }

    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    @Override
    public String getCode() {
        return errorMessage != null ? errorMessage.getCode() : StringUtils.EMPTY;
    }

    @Override
    public String getOrigin() {
        return "remote";
    }

    @Override
    public String getMessage() {
        return errorMessage != null ? errorMessage.getMessage() : super.getMessage();
    }
}