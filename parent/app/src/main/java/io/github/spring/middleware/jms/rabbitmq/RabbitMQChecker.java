package io.github.spring.middleware.jms.rabbitmq;

import io.github.spring.middleware.rabbitmq.core.JmsResources;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@ConditionalOnProperty(name = "middleware.jms.rabbitmq.baseUrl")
public class RabbitMQChecker {

    @Autowired(required = false)
    private JmsResources jmsResources;

    @Autowired
    private RabbitMQClient rabbitMQClient;


    public void checkConsumers() {
        if (jmsResources == null) {
            return;
        }

        rabbitMQClient.getConsumers()
                .collectList()
                .subscribe(
                        rabbitConsumers -> jmsResources.getAllConsumers().forEach(consumerResource -> {
                            if (!isActive(rabbitConsumers,
                                    consumerResource.getJmsResourceDestination().getDestinationName())) {
                                CompletableFuture.runAsync(() -> consumerResource.start(true));
                            }
                        }),
                        ex -> log.error("Error checking RabbitMQ consumers", ex)
                );
    }

    private boolean isActive(Collection<RabbitConsumerData> rabbitConsumers, String queueName) {
        return rabbitConsumers.stream()
                .anyMatch(c -> c.getQueue() != null
                        && c.getQueue().getName() != null
                        && c.getQueue().getName().equalsIgnoreCase(queueName)
                        && c.isActive());
    }
}