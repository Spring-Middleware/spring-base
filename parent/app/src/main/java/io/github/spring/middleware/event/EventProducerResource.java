package io.github.spring.middleware.event;

import io.github.spring.middleware.jms.JmsActiveProfileSuffix;
import io.github.spring.middleware.jms.annotations.NotifyErrorHandler;
import io.github.spring.middleware.rabbitmq.annotations.JmsDestination;
import io.github.spring.middleware.rabbitmq.annotations.JmsProducer;
import io.github.spring.middleware.rabbitmq.core.resource.producer.JmsProducerResource;
import org.springframework.stereotype.Component;

@Component
@JmsProducer
@NotifyErrorHandler
@JmsDestination(name = "event-queue", clazzSuffix = JmsActiveProfileSuffix.class)
public class EventProducerResource extends JmsProducerResource<EventRequest> {

}
