package io.github.spring.middleware.rabbitmq.resources.listener;



import io.github.spring.middleware.rabbitmq.annotations.JmsListener;
import io.github.spring.middleware.rabbitmq.core.resource.listener.JmsResourceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

@JmsListener
public class JmsListenerAll implements JmsResourceListener {

    private Logger logger = LoggerFactory.getLogger(JmsListenerAll.class);

    @Override
    public void onBeforeProcessingMessage(Properties properties) {
        logger.info("[ALL] Listener on before process message");
    }
}
