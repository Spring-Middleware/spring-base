package io.github.spring.middleware.graphql.gateway.runtime;

import io.github.spring.middleware.client.config.ProxyClientsReadyEvent;
import io.github.spring.middleware.graphql.gateway.factory.GraphQLGatewayFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GraphQLGatewayInitializer {

    private final GraphQLGatewayFactory graphQLGatewayFactory;
    private final GraphQLGatewayHolder holder;

    @EventListener(ProxyClientsReadyEvent.class)
    public void initialize() {
        holder.initialize(graphQLGatewayFactory.build());
    }
}
