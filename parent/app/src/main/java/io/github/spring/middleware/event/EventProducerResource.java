package io.github.spring.middleware.event;

import io.github.spring.middleware.jms.JmsActiveProfileSuffix;
import io.github.spring.middleware.jms.annotations.NotifyErrorHandler;
import com.middleware.jms.annotations.JmsDestination;
import com.middleware.jms.annotations.JmsProducer;
import com.middleware.jms.core.resource.producer.JmsProducerResource;
import org.springframework.stereotype.Component;

@Component
@JmsProducer
@NotifyErrorHandler
@JmsDestination(name = "event-queue", clazzSuffix = JmsActiveProfileSuffix.class)
public class EventProducerResource extends JmsProducerResource<EventRequest> {

}
