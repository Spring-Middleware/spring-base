package io.github.spring.middleware.registry.jms;

import io.github.spring.middleware.jms.client.ProxyClientEvent;
import io.github.spring.middleware.rabbitmq.annotations.JmsBinding;
import io.github.spring.middleware.rabbitmq.annotations.JmsDestination;
import io.github.spring.middleware.rabbitmq.annotations.JmsProducer;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationType;
import io.github.spring.middleware.rabbitmq.core.resource.producer.JmsProducerResource;
import org.springframework.stereotype.Component;

@Component
@JmsProducer(bindings = {@JmsBinding(routingKey = "client-events.refresh")})
@JmsDestination(name = "registry", destinationType = DestinationType.TOPIC)
public class ProxyClientEventResourceProducer extends JmsProducerResource<ProxyClientEvent> {

}
