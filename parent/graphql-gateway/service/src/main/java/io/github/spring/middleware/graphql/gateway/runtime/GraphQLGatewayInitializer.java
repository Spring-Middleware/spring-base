package io.github.spring.middleware.graphql.gateway.runtime;

import graphql.GraphQL;
import io.github.spring.middleware.client.config.ProxyClientsReadyEvent;
import io.github.spring.middleware.graphql.gateway.factory.GraphQLGatewayFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GraphQLGatewayInitializer {

    private final GraphQLGatewayFactory graphQLGatewayFactory;
    private final GraphQLGatewayHolder holder;

    @EventListener(ProxyClientsReadyEvent.class)
    public void initialize() {
        holder.initialize(graphQLGatewayFactory.build());
    }

    public void refresh() {
        try {
            final GraphQL graphQL = graphQLGatewayFactory.build();
            holder.refresh(graphQL);
            log.info("Successfully refreshed GraphQL instance for manual refresh");
        } catch (Exception exception) {
            log.error("Failed to build GraphQL instance for manual refresh", exception);
            throw exception;
        }
    }
}
