package io.github.spring.middleware.graphql.gateway.jms;

import graphql.GraphQL;
import io.github.spring.middleware.graphql.gateway.event.SchemaLocationEvent;
import io.github.spring.middleware.graphql.gateway.event.SchemaLocationEventType;
import io.github.spring.middleware.graphql.gateway.factory.GraphQLGatewayFactory;
import io.github.spring.middleware.graphql.gateway.runtime.GraphQLGatewayHolder;
import io.github.spring.middleware.jms.client.NodeIdBasedSuffix;
import io.github.spring.middleware.rabbitmq.annotations.JmsConsumer;
import io.github.spring.middleware.rabbitmq.annotations.JmsDestination;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationType;
import io.github.spring.middleware.rabbitmq.core.resource.consumer.JmsConsumerResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
@JmsConsumer
@JmsDestination(name = "graphql-events", destinationType = DestinationType.QUEUE, clazzSuffix = NodeIdBasedSuffix.class, expires = 60000)
public class SchemaLocationEventConsumerResource extends JmsConsumerResource<SchemaLocationEvent> {

    private final GraphQLGatewayFactory graphQLGatewayFactory;
    private final GraphQLGatewayHolder graphQLGatewayHolder;

    @Override
    public void process(SchemaLocationEvent schemaLocationEvent, Properties properties) throws Exception {
        final SchemaLocationEventType eventType = schemaLocationEvent.getEventType();
        final String namespace = schemaLocationEvent.getNamespace();
        if (eventType == SchemaLocationEventType.REFRESH) {
            try {
                final GraphQL graphQL = graphQLGatewayFactory.build();
                graphQLGatewayHolder.refresh(graphQL);
                log.info("Successfully refreshed GraphQL instance for namespace: {}", namespace);
            } catch (Exception exception) {
                log.error("Failed to build GraphQL instance for namespace: {}", namespace, exception);
                throw exception;
            }
        }
    }
}
