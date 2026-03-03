package io.github.spring.middleware.error;

public interface RemoteError extends ErrorDescriptor {

    int getHttpStatusCode();

    String getRequestId();

    default String getOrigin() {
        return "unknown";
    }

    default Object getPayload() {
        return null;
    }
}
