package io.github.spring.middleware.rabbitmq.resources.listener;


import io.github.spring.middleware.rabbitmq.annotations.JmsListener;
import io.github.spring.middleware.rabbitmq.annotations.listener.JmsAllProducers;
import io.github.spring.middleware.rabbitmq.core.resource.listener.JmsResourceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;


@JmsListener(value = JmsAllProducers.class, priority = 1)
public class JmsProducerListenerAll implements JmsResourceListener {

    private Logger logger = LoggerFactory.getLogger(JmsProducerListenerAll.class);

    @Override
    public void onBeforeProcessingMessage(Properties properties) {
        logger.info("[PRODUCER] Listener before send message");
    }
}
