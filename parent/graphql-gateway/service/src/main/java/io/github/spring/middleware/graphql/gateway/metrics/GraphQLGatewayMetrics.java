package io.github.spring.middleware.graphql.gateway.metrics;

import graphql.GraphQLContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GraphQLGatewayMetrics {

    private final MeterRegistry meterRegistry;
    private final GraphQLMetricsModeResolver metricsModeResolver;


    public Timer.Sample startRemoteExecutionTimer(GraphQLContext context) {
        if (!metricsModeResolver.isEnabled(context)) {
            return null;
        }
        return Timer.start(meterRegistry);
    }


    public void recordRemoteExecution(GraphQLContext context,
                                      String schema,
                                      String operation,
                                      String outcome,
                                      Timer.Sample sample) {
        if (!metricsModeResolver.isEnabled(context) || sample == null) {
            return;
        }

        sample.stop(
                Timer.builder("graphql.gateway.remote.execution")
                        .tag("schema", sanitize(schema))
                        .tag("operation", sanitize(operation))
                        .tag("outcome", sanitize(outcome))
                        .register(meterRegistry)
        );
    }


    private String sanitize(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

}
