package io.github.spring.middleware.client.proxy;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MiddlewareCircuitBreakerParameters {

    private boolean enanbled;
    private float failureRateThreshold;
    private int minimumNumberOfCalls;
    private int slidingWindowSize;
    private int permittedNumberOfCallsInHalfOpenState;
    private long waitDurationInOpenStateMs;
    private List<String> openCircuitBreakerStatusExpressions = new ArrayList<>();
    private List<String> ignoreCircuitBreakerStatusExpressions = new ArrayList<>();
}
