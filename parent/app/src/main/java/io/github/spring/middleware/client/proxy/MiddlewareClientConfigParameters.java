package io.github.spring.middleware.client.proxy;

import lombok.Data;

@Data
public class MiddlewareClientConfigParameters {

    private MiddlewareClientConnectionParameters connectionParameters;
    private MiddlewareCircuitBreakerParameters circuitBreakerParameters;


}
