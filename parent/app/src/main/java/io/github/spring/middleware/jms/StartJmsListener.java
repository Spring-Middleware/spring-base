package io.github.spring.middleware.jms;

import io.github.spring.middleware.component.NodeInfoRetriever;
import io.github.spring.middleware.jms.rabbitmq.CreateBindingRequest;
import io.github.spring.middleware.jms.rabbitmq.CreateExchangeRequest;
import io.github.spring.middleware.jms.rabbitmq.ExchangeData;
import io.github.spring.middleware.jms.rabbitmq.RabbitMQClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartJmsListener {

    private final RabbitMQClient rabbitMQClient;
    private final NodeInfoRetriever nodeInfoRetriever;

    @EventListener(ApplicationStartedEvent.class)
    public void startJmsListener() {

        rabbitMQClient.getExchange("registry")
                .switchIfEmpty(Mono.defer(() -> {
                    CreateExchangeRequest exchangeRequest = new CreateExchangeRequest();
                    exchangeRequest.setType("topic");
                    exchangeRequest.setDurable(true);
                    exchangeRequest.setAutoDelete(false);
                    return rabbitMQClient.createExchange("registry", exchangeRequest)
                            .thenReturn(new ExchangeData("registry", null, null));
                }))
                .flatMap(exchangeData -> {
                    CreateBindingRequest bindingRequest = new CreateBindingRequest();
                    bindingRequest.setRoutingKey(STR."client-events.#");
                    return rabbitMQClient.createBinding(
                            exchangeData.getName(),
                            STR."client-events-\{nodeInfoRetriever.getNodeClusterAndId()}",
                            bindingRequest
                    );
                })
                .subscribe(
                        ignored -> log.info("Successfully ensured registry exchange and binding"),
                        error -> log.error("Failed to ensure registry exchange and binding", error)
                );
    }
}
