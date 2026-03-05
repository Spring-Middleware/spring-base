package io.github.spring.middleware.client.proxy;

import io.github.spring.middleware.error.ErrorMessage;
import io.github.spring.middleware.error.RemoteError;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RemoteServerException extends RuntimeException implements RemoteError {

    private final int httpStatusCode;
    private ErrorMessage errorMessage;
    private Map<String, Object> extensions = new HashMap<>();


    public RemoteServerException(ErrorMessage errorMessage, int httpStatusCode, String requestId) {
        this(String.valueOf(errorMessage), null, httpStatusCode, requestId);
        this.errorMessage = errorMessage;
    }

    public RemoteServerException(String message, Exception e, int httpStatusCode, String requestId) {

        super(message, e);
        this.httpStatusCode = httpStatusCode;
        this.extensions.put("request-id", requestId);
    }


    public String getRequestId() {

        return Optional.ofNullable((String) this.extensions.get("request-id")).orElse(StringUtils.EMPTY);
    }

    @Override
    public Object getPayload() {
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
        return "";
    }

    @Override
    public String getOrigin() {
        return "";
    }
}
