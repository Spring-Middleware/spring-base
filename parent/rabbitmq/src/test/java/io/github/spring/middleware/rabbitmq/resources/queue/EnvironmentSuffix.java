package io.github.spring.middleware.rabbitmq.resources.queue;


import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationSuffix;

public class EnvironmentSuffix implements DestinationSuffix {


    @Override
    public String version() {
        return "DEV";
    }
}
