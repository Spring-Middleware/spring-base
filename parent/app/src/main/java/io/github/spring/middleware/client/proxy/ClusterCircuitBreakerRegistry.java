package io.github.spring.middleware.client.proxy;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;

@Component
public class ClusterCircuitBreakerRegistry {

    private final CircuitBreakerRegistry registry;

    private static Integer DEFAULT_SLOW_CALL_DURATION_THRESHOLD_MS = 2000;
    private static Float DEFAULT_SLOW_CALL_RATE_THRESHOLD = 50.0f;

    public ClusterCircuitBreakerRegistry() {
        this.registry = CircuitBreakerRegistry.ofDefaults();
    }

    public CircuitBreaker getOrCreate(
            final String breakerName,
            final MiddlewareCircuitBreakerParameters circuitBreakerParameters) {

        return registry.circuitBreaker(breakerName, buildConfig(circuitBreakerParameters));
    }

    protected CircuitBreakerConfig buildConfig(
            final MiddlewareCircuitBreakerParameters circuitBreakerParameters) {

        return CircuitBreakerConfig.custom()
                .failureRateThreshold(circuitBreakerParameters.getFailureRateThreshold())
                .minimumNumberOfCalls(circuitBreakerParameters.getMinimumNumberOfCalls())
                .slidingWindowSize(circuitBreakerParameters.getSlidingWindowSize())
                .permittedNumberOfCallsInHalfOpenState(
                        circuitBreakerParameters.getPermittedNumberOfCallsInHalfOpenState())
                .waitDurationInOpenState(Duration.ofMillis(
                        circuitBreakerParameters.getWaitDurationInOpenStateMs()))
                .slowCallDurationThreshold(Duration.ofMillis(DEFAULT_SLOW_CALL_DURATION_THRESHOLD_MS))
                .slowCallRateThreshold(DEFAULT_SLOW_CALL_RATE_THRESHOLD)
                .recordException(this.shouldRecordException(circuitBreakerParameters))
                .ignoreException(this.shouldIgnoreException(circuitBreakerParameters))
                .build();
    }

    protected Predicate<Throwable> shouldRecordException(MiddlewareCircuitBreakerParameters circuitBreakerParameters) {
        return throwable -> {
            if (throwable instanceof ProxyClientException) {
                return true;
            }

            if (throwable instanceof RemoteServerException rse) {
                int status = rse.getHttpStatusCode();
                return matchesExpressions(circuitBreakerParameters.getOpenCircuitBreakerStatusExpressions(), status);
            }
            return false;

        };
    }

    protected Predicate<Throwable> shouldIgnoreException(MiddlewareCircuitBreakerParameters circuitBreakerParameters) {
        return throwable -> {
            if (throwable instanceof RemoteServerException rse) {
                int status = rse.getHttpStatusCode();
                return matchesExpressions(circuitBreakerParameters.getIgnoreCircuitBreakerStatusExpressions(), status);
            }
            return false;
        };
    }

    private boolean matchesExpressions(List<String> expressions, int status) {
        return expressions.stream().anyMatch(e -> matches(status, e));
    }

    private boolean matches(int status, String expression) {
        String s = String.valueOf(status);

        if (expression.length() == 3) {
            for (int i = 0; i < 3; i++) {
                char ec = expression.charAt(i);
                if (ec != 'x' && ec != 'X' && ec != s.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
        return s.equals(expression);
    }

}

