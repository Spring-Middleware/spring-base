package io.github.spring.middleware.jms;

import io.github.spring.middleware.component.NodeInfoRetriever;
import io.github.spring.middleware.jms.rabbitmq.CreateBindingRequest;
import io.github.spring.middleware.jms.rabbitmq.CreateExchangeRequest;
import io.github.spring.middleware.jms.rabbitmq.CreateQueueRequest;
import io.github.spring.middleware.jms.rabbitmq.ExchangeData;
import io.github.spring.middleware.jms.rabbitmq.RabbitMQClient;
import io.github.spring.middleware.jms.rabbitmq.RabbitQueueData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ScheduledExecutorTask;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegistryMessagingBootstrap {

    private final RabbitMQClient rabbitMQClient;
    private final NodeInfoRetriever nodeInfoRetriever;

    @Scheduled(fixedDelayString = "${middleware.jms.rabbitmq.check-interval:10000}")
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
                    String queueName = STR."client-events-\{nodeInfoRetriever.getNodeClusterAndId()}";

                    return rabbitMQClient.getDestinationQueue(queueName)
                            .switchIfEmpty(Mono.defer(() -> {
                                CreateQueueRequest queueRequest = new CreateQueueRequest();
                                queueRequest.setQueueName(queueName);
                                queueRequest.setDurable(false);
                                queueRequest.setAutoDelete(false);
                                queueRequest.setArguments(Map.of("x-expires", 60000));

                                return rabbitMQClient.createQueue(queueName, queueRequest)
                                        .thenReturn(new RabbitQueueData(queueName, "/"));
                            }))
                            .thenReturn(exchangeData);
                })
                .flatMap(exchangeData -> {
                    String queueName = STR."client-events-\{nodeInfoRetriever.getNodeClusterAndId()}";

                    return rabbitMQClient.getBindingForExchange(exchangeData.getName(), queueName)
                            .switchIfEmpty(Mono.defer(() -> {
                                CreateBindingRequest bindingRequest = new CreateBindingRequest();
                                bindingRequest.setRoutingKey("client-events.#");

                                return rabbitMQClient.createBinding(
                                        exchangeData.getName(),
                                        queueName,
                                        bindingRequest
                                ).then(Mono.empty());
                            }))
                            .then();
                })
                .subscribe(
                        ignored -> log.info("Successfully ensured registry exchange, queue and binding"),
                        error -> log.error("Failed to ensure registry exchange, queue and binding", error)
                );
    }
}
