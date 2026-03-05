package io.github.spring.middleware.client.proxy;

import lombok.Data;

@Data
public class MiddlewareClientConnectionParameters {

    private int timeout;
    private int maxConnections;
    private int maxConcurrentCalls;
    private int maxRetries;
    private int retryBackoffMillis;
}
