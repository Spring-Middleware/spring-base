package com.core.event;

import com.core.jms.JmsActiveProfileSuffix;
import com.core.jms.annotations.NotifyErrorHandler;
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
