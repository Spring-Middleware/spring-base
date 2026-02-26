package io.github.spring.middleware.config;

import io.github.spring.middleware.rabbitmq.core.JmsResources;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JmsConsumersManager {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private JmsConfiguration jmsConfiguration;

    @EventListener(ApplicationReadyEvent.class)
    public void startComsumers() {

        if (!CollectionUtils.emptyIfNull(jmsConfiguration.getBasePackages()).isEmpty()) {
            JmsResources jmsResources = applicationContext.getBean(JmsResources.class);
            if (jmsResources != null) {
                log.info("Starting jms consumers ");
                jmsResources.getAllConsumers().stream().forEach(jmsConsumerResource -> {
                    jmsConsumerResource.start(false);
                });
            }
        }
    }

    @EventListener(ContextClosedEvent.class)
    public void stopConsumers() {

        if (!CollectionUtils.emptyIfNull(jmsConfiguration.getBasePackages()).isEmpty()) {
            JmsResources jmsResources = applicationContext.getBean(JmsResources.class);
            if (jmsResources != null) {
                log.info("Stopping jms consumers ");
                jmsResources.getAllConsumers().stream().forEach(jmsConsumerResource -> {
                    jmsConsumerResource.stop(true);
                });
            }
        }
    }

}
