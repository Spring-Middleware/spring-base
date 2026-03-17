package io.github.spring.middleware.registry.jms;

import io.github.spring.middleware.graphql.gateway.event.SchemaLocationEvent;
import io.github.spring.middleware.rabbitmq.annotations.JmsBinding;
import io.github.spring.middleware.rabbitmq.annotations.JmsDestination;
import io.github.spring.middleware.rabbitmq.annotations.JmsProducer;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationType;
import io.github.spring.middleware.rabbitmq.core.resource.producer.JmsProducerResource;
import org.springframework.stereotype.Component;

@Component
@JmsProducer(bindings = {@JmsBinding(routingKey = "graphql-events.refresh")})
@JmsDestination(name = "graphql", destinationType = DestinationType.TOPIC)
public class SchemaLocationEventResourceProducer extends JmsProducerResource<SchemaLocationEvent> {
}
