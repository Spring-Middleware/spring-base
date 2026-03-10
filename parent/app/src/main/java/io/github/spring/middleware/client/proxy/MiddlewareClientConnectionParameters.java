package io.github.spring.middleware.client.proxy;

import lombok.Data;

@Data
public class MiddlewareClientConnectionParameters {

    private int timeout;
    private int maxConnections;
    private int maxConcurrentCalls;
    private int maxRetries;
    private int retryBackoffMillis;

    public static MiddlewareClientConnectionParameters defaultParameters() {
        MiddlewareClientConnectionParameters parameters = new MiddlewareClientConnectionParameters();
        parameters.setTimeout(30000);
        parameters.setMaxConnections(50);
        parameters.setMaxConcurrentCalls(200);
        parameters.setMaxRetries(3);
        parameters.setRetryBackoffMillis(1000);
        return parameters;
    }
}
