package io.github.spring.middleware.jms.rabbitmq;

import lombok.Data;

import java.util.Map;

@Data
public class CreateQueueRequest {

    private String queueName;
    private boolean durable;
    private boolean exclusive;
    private boolean autoDelete;
    private Map<String, Object> arguments;

}
