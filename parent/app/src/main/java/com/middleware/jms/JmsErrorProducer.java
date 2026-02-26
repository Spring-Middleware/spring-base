package com.middleware.jms;

import com.middleware.error.api.ErrorRequest;
import com.middleware.jms.annotations.JmsDestination;
import com.middleware.jms.annotations.JmsProducer;
import com.middleware.jms.core.resource.producer.JmsProducerResource;
import org.springframework.stereotype.Component;

@Component
@JmsProducer
@JmsDestination(name = "queue-error", clazzSuffix = JmsActiveProfileSuffix.class)
public class JmsErrorProducer extends JmsProducerResource<ErrorRequest> {

}
