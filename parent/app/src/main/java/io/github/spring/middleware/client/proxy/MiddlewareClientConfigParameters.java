package io.github.spring.middleware.client.proxy;

import io.github.spring.middleware.client.proxy.security.config.SecurityClientConfiguration;
import lombok.Data;

@Data
public class MiddlewareClientConfigParameters {

    private MiddlewareClientConnectionParameters connectionParameters;
    private MiddlewareCircuitBreakerParameters circuitBreakerParameters;
    private SecurityClientConfiguration securityClientConfiguration;

}
