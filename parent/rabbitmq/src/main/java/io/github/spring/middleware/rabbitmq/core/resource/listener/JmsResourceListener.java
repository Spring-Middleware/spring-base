package io.github.spring.middleware.rabbitmq.core.resource.listener;

import java.util.Properties;

public interface JmsResourceListener {

    void onBeforeProcessingMessage(Properties properties);

}
