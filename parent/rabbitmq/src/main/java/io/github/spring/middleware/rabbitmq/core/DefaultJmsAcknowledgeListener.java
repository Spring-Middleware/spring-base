package io.github.spring.middleware.rabbitmq.core;

import jakarta.jms.Message;

public class DefaultJmsAcknowledgeListener implements JmsAcknowledgeListener {
    @Override
    public void acknowledge(Message message) throws Exception {
        message.acknowledge();
    }
}
