package io.github.spring.middleware.rabbitmq.core;


import jakarta.jms.Message;

public interface JmsAcknowledgeListener {

    void acknowledge(Message message);

}
