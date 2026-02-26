package io.github.spring.middleware.jms.rabbitmq;

import com.middleware.jms.annotations.JmsBinding;
import com.middleware.jms.annotations.JmsDestination;
import com.middleware.jms.annotations.JmsProducer;
import com.middleware.jms.core.JmsResources;
import com.middleware.jms.core.destination.type.DestinationSuffix;
import com.middleware.jms.core.destination.type.DestinationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class RabbitMQChecker {

    @Autowired(required = false)
    private JmsResources jmsResources;
    @Autowired
    private RabbitMQClient rabbitMQClient;

    @Scheduled(fixedDelayString = "${jms.rabbitmq.fixedDelay:30000}")
    public void scheduledCheckConsumers() {

        checkConsumers();
    }

    @Scheduled(fixedDelayString = "${jms.rabbitmq.fixedDelay:30000}")
    public void scheduledCheckBindings() {

        checkBindings();
    }

    public void checkConsumers() {

        Flux<RabbitConsumerData> flux = rabbitMQClient.getConsumers();
        if (flux != null && jmsResources != null) {
            flux.collectList().subscribe(rabbitConsumers -> {
                jmsResources.getAllConsumers().stream().forEach(consumerResource -> {
                    if (!isActive(rabbitConsumers, consumerResource.getJmsResourceDestination().getDestinationName())) {
                        CompletableFuture.runAsync(() -> consumerResource.start(true));
                    }
                });
            });
        }
    }

    public void checkBindings() {

        if (jmsResources != null) {
            jmsResources.getAllProducers().stream()
                    .filter(c -> c.getJmsResourceDestination().getDestinationType() == DestinationType.TOPIC)
                    .forEach(topic -> {
                        JmsProducer jmsProducer = topic.getClass().getAnnotation(JmsProducer.class);
                        JmsDestination jmsDestination = topic.getJmsResourceDestination().getJmsDestination();
                        String exchangeName = getExchangeName(jmsDestination);
                        Optional.ofNullable(exchangeName)
                                .ifPresent(exName -> checkExchangeBindings(jmsDestination, jmsProducer, exchangeName));
                    });
        }
    }

    private void checkExchangeBindings(JmsDestination jmsDestination, JmsProducer jmsProducer, String exchangeName) {

        getExchange(exchangeName).doOnNext(exchangeData -> {
            log.debug("Checking bindings for exchange " + exchangeData.getName());
            Flux<RabbitBindingData> fluxCurrentBindings = rabbitMQClient.getBindingsForExchange(exchangeName);
            createBindingsForExchange(fluxCurrentBindings, exchangeName, jmsProducer, jmsDestination);
        }).subscribe(v -> {
            log.debug("Exchange checked " + exchangeName);
        });
    }

    private Mono<ExchangeData> getExchange(String exchangeName) {

        return rabbitMQClient.getExchange(exchangeName)
                .filter(data -> data.getName() != null && data.getName().equals(exchangeName))
                .onErrorReturn(createExchange(exchangeName));
    }

    private ExchangeData createExchange(String exchangeName) {

        return rabbitMQClient.createExchange(exchangeName,
                        CreateExchangeRequest.builder().type("topic").autoDelete(false).durable(false)
                                .internal(false)
                                .build()).map(v -> ExchangeData.builder().name(exchangeName).build())
                .doOnSuccess(v -> log.info("Created exchange " + exchangeName)).block();
    }

    private void createBindingsForExchange(Flux<RabbitBindingData> fluxCurrentBindings, String exchangeName,
            JmsProducer jmsProducer, JmsDestination jmsDestination) {

        fluxCurrentBindings.collectList().subscribe(currentBindings -> {
            Arrays.stream(jmsProducer.bindings()).forEach(binding -> {
                String destinationQueueName = Optional.ofNullable(getDestinationQueueName(jmsDestination, binding))
                        .orElse(null);

                if (destinationQueueName != null &&
                        !existsBinding(currentBindings, destinationQueueName, binding.routingKey())) {
                    createBinding(binding, exchangeName, destinationQueueName);
                } else {
                    log.debug("Binding " + destinationQueueName + "@" + binding.routingKey() +
                            " already exists in exchange " + exchangeName);
                }
            });
        });
    }

    private void createBinding(JmsBinding binding, String exchangeName, String destinationQueueName) {

        rabbitMQClient.createBinding(exchangeName, destinationQueueName,
                CreateBindingRequest.builder().routingKey(binding.routingKey()).build()).doOnSuccess(v -> log.info(
                "Binding created " + binding.routingKey() + " to " + destinationQueueName + " for exchange " +
                        exchangeName)).subscribe();
    }

    private String getDestinationQueueName(JmsDestination jmsDestination, JmsBinding binding) {

        String destinationQueueName = null;
        try {
            DestinationSuffix destinationSuffix = jmsDestination.clazzSuffix().newInstance();
            destinationQueueName = binding.destinationQueue() + "-" + destinationSuffix.version();
        } catch (Exception ex) {
            log.error("Can't obtain destination queue name " + binding.destinationQueue());
        }
        return destinationQueueName;
    }

    private String getExchangeName(JmsDestination jmsDestination) {

        String exchangeName = null;
        try {
            DestinationSuffix destinationSuffix = jmsDestination.clazzSuffix().newInstance();
            exchangeName = jmsDestination.exchange() + "-" + destinationSuffix.version();
        } catch (Exception ex) {
            log.error("Can't obtain exchangeName " + jmsDestination.exchange());
        }
        return exchangeName;
    }

    private boolean existsBinding(Collection<RabbitBindingData> currentBindings, String destinationQueueName,
            String routingKey) {

        return currentBindings.stream()
                .anyMatch(c -> c.getDestination().equals(destinationQueueName) && c.getRoutingKey().equals(routingKey));
    }

    private boolean isActive(Collection<RabbitConsumerData> rabbitConsumers, String queueName) {

        return rabbitConsumers.stream()
                .anyMatch(c -> c.getQueue().getName().equalsIgnoreCase(queueName) && c.isActive());

    }

}
