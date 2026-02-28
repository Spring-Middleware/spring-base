package io.github.spring.middleware.client.proxy;

import io.github.spring.middleware.client.error.ErrorResponse;
import io.github.spring.middleware.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RemoteServerException extends RuntimeException {

    private final int httpStatusCode;
    private ErrorResponse errorResponse;
    private Map<String, Object> extensions = new HashMap<>();
    private String errorCode;

    public RemoteServerException(Exception e, int httpStatusCode, String requestId) {

        this(ExceptionUtils.getStackTrace(e, 2), e, httpStatusCode, requestId);
    }

    public RemoteServerException(String message, int httpStatusCode, String requestId) {

        this(message, null, httpStatusCode, requestId);
    }

    public RemoteServerException(String message, Exception e, int httpStatusCode, String requestId) {

        super(message, e);
        this.httpStatusCode = httpStatusCode;
        this.extensions.put("request-id", requestId);
    }

    public RemoteServerException(ErrorResponse errorResponse, int httpStatusCode, String requestId) {

        super(errorResponse.getErrorSystemMessage());
        this.errorResponse = errorResponse;
        this.httpStatusCode = httpStatusCode;
        this.errorCode = errorResponse.getErrorCode();
        this.extensions.put("errorCode", this.errorCode);
        this.extensions.put("request-id", requestId);
        this.extensions.put("server-error", errorResponse.getServer());
        List<Map<String, Object>> details = errorResponse.getDetails();
        details.stream().forEach(detail -> this.extensions.putAll(detail));
    }

    public String getRequestId() {

        return Optional.ofNullable((String) this.extensions.get("request-id")).orElse(StringUtils.EMPTY);
    }

    public String getErrorCode() {

        return this.errorCode;
    }

    public Map<String, Object> getExtensions() {

        return extensions;
    }


    public int getHttpStatusCode() {

        return httpStatusCode;
    }

    public ErrorResponse getErrorResponse() {

        return Optional.ofNullable(errorResponse).orElse(ErrorResponse.nok(getMessage()));
    }

    public void setErrorResponse(ErrorResponse errorResponse) {

        this.errorResponse = errorResponse;
    }
}
