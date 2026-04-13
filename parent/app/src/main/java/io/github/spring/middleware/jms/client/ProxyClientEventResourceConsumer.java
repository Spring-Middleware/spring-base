package io.github.spring.middleware.jms.client;

import io.github.spring.middleware.client.config.ProxyClientResilienceConfigurator;
import io.github.spring.middleware.client.proxy.ProxyClient;
import io.github.spring.middleware.client.proxy.ProxyClientRegistry;
import io.github.spring.middleware.rabbitmq.annotations.JmsConsumer;
import io.github.spring.middleware.rabbitmq.annotations.JmsDestination;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationType;
import io.github.spring.middleware.rabbitmq.core.resource.consumer.JmsConsumerResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
@JmsConsumer
@JmsDestination(name = "client-events", destinationType = DestinationType.QUEUE, clazzSuffix = NodeIdBasedSuffix.class, expires = 60000)
@ConditionalOnBean(ProxyClientResilienceConfigurator.class)
public class ProxyClientEventResourceConsumer extends JmsConsumerResource<ProxyClientEvent> {

    private final ProxyClientResilienceConfigurator clientResilienceConfigurator;

    @Override
    public void process(ProxyClientEvent proxyClientEvent, Properties properties) throws Exception {
        switch (proxyClientEvent.getEventType()) {
            case CLIENT_UNAVAILABLE ->
                    clientResilienceConfigurator.desconfigureClient(proxyClientEvent.getClientName());
            case CLIENT_AVAILABLE -> {
                ProxyClient<?> proxyClient = ProxyClientRegistry.getByName(proxyClientEvent.getClientName());
                if (proxyClient != null) {
                    clientResilienceConfigurator.configureProxy(proxyClient);
                } else {
                    log.info("Received CLIENT_AVAILABLE event for client {} but no ProxyClient found with that name", proxyClientEvent.getClientName());
                }
            }
        }
    }

}
