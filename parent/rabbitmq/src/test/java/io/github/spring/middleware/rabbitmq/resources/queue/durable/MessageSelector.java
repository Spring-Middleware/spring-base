package io.github.spring.middleware.rabbitmq.resources.queue.durable;


import io.github.spring.middleware.rabbitmq.core.JmsSelector;

import java.util.Properties;

public class MessageSelector implements JmsSelector {


    @Override
    public Properties properties() {
        Properties properties = new Properties();
        properties.setProperty("ENVIRONMENT", "DEV");
        return properties;
    }
}
