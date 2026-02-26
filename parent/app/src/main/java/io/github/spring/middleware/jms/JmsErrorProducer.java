package io.github.spring.middleware.jms;

import io.github.spring.middleware.error.api.ErrorRequest;
import io.github.spring.middleware.rabbitmq.annotations.JmsDestination;
import io.github.spring.middleware.rabbitmq.annotations.JmsProducer;
import io.github.spring.middleware.rabbitmq.core.resource.producer.JmsProducerResource;
import org.springframework.stereotype.Component;

@Component
@JmsProducer
@JmsDestination(name = "queue-error", clazzSuffix = JmsActiveProfileSuffix.class)
public class JmsErrorProducer extends JmsProducerResource<ErrorRequest> {

}
