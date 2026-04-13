package io.github.spring.middleware.jms;

import io.github.spring.middleware.jms.rabbitmq.RabbitMQClient;
import io.github.spring.middleware.rabbitmq.core.JmsResources;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(JmsResources.class)
public class ClusterMessagingShutdown {

    private final JmsResources jmsResources;
    private final RabbitMQClient rabbitMQClient;

    @PreDestroy
    public void shutdown() {
        jmsResources.getAllConsumers().stream().forEach(jmsConsumerResource -> {
            jmsConsumerResource.stop(true);
            rabbitMQClient.deleteQueue(jmsConsumerResource.getJmsResourceDestination().getDestinationName())
                    .subscribe(success -> log.info(STR."Deleted queue: \{jmsConsumerResource.getJmsResourceDestination().getDestinationName()}"),
                            error -> log.error(STR."Error deleting queue: \{jmsConsumerResource.getJmsResourceDestination().getDestinationName()}"));
        });
    }

}
