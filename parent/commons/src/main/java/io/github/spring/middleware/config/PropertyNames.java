package io.github.spring.middleware.config;

public final class PropertyNames {

    public static final String REQUEST_ID = "X-Request-Id";
    public static final String SPAN_ID = "X-Span-Id";
    public static final String LOGGING_KEY = "X-Logging-Key";
    public static final String RESPONSE_TIME_LOG = "Response-Time-Log";
    public static final String CONTENT_LANGUAGE = "Content-Language";
    public static final String HEADERS_TO_COPY = "Headers";
    public static final String REQUEST_HEADERS = "Request-Headers";

    private PropertyNames() {
        // Utility class - prevent instantiation
    }

}
