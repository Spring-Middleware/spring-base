package io.github.spring.middleware.jms.rabbitmq;

import io.github.spring.middleware.config.JmsConfiguration;
import io.github.spring.middleware.http.AbstractWebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "middleware.jms.rabbitmq", name = "base-url")
public class RabbitMQClient extends AbstractWebClient {

    @Value("${middleware.jms.rabbitmq.base-url:#{null}}")
    private String baseUrl;

    private final JmsConfiguration jmsConfiguration;

    @Override
    protected String getBaseUrl() {
        return baseUrl;
    }

    @Override
    protected ExchangeFilterFunction getExchangeFilterFunction() {
        return ExchangeFilterFunctions.basicAuthentication(
                jmsConfiguration.getUser(),
                jmsConfiguration.getPassword()
        );
    }


    public Mono<RabbitQueueData> getDestinationQueue(String queueName) {

        return client()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/queues/{vhost}/{queueName}")
                        .build("/", queueName))
                .retrieve()
                .bodyToMono(RabbitQueueData.class)
                .doOnError(ex -> log.error("Can't connect with {} for read queue={}", baseUrl, queueName, ex))
                .doOnSuccess(v -> log.info("Read queue {} queueName={}", baseUrl, queueName))
                .onErrorResume(this::emptyOnClientErrorMono);
    }

    public Mono<Void> createQueue(String queueName, CreateQueueRequest createQueueRequest) {

        return client()
                .put()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/queues/{vhost}/{queueName}")
                        .build("/", queueName))
                .bodyValue(createQueueRequest)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(ex -> log.error("Can't create queue {} queueName={}", baseUrl, queueName, ex))
                .doOnSuccess(v -> log.info("Created queue {} queueName={}", baseUrl, queueName))
                .onErrorResume(this::emptyOnClientErrorMono);
    }


    public Flux<RabbitConsumerData> getConsumers() {

        return client()
                .get()
                .uri("/api/consumers")
                .retrieve()
                .bodyToFlux(RabbitConsumerData.class)
                .doOnError(ex -> log.error("Can't connect with {}", baseUrl, ex))
                .onErrorResume(this::emptyOnClientErrorFlux);
    }

    public Mono<RabbitBindingData> getBindingForExchange(String exchangeName, String destinationQueue) {

        return client()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/exchanges/{vhost}/{exchangeName}/bindings/source")
                        .build("/", exchangeName))
                .retrieve()
                .bodyToFlux(RabbitBindingData.class)
                .filter(binding -> destinationQueue.equals(binding.getDestination()))
                .next()
                .doOnError(ex -> log.error("Can't connect with {} for read binding exchange={} destinationQueue={}",
                        baseUrl, exchangeName, destinationQueue, ex))
                .doOnSuccess(v -> {
                    if (v != null) {
                        log.info("Read binding {} exchangeName={} destinationQueue={}",
                                baseUrl, exchangeName, destinationQueue);
                    }
                })
                .onErrorResume(this::emptyOnClientErrorMono);
    }



    public Mono<ExchangeData> getExchange(String exchangeName) {

        return client()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/exchanges/{vhost}/{exchangeName}")
                        .build("/", exchangeName))
                .retrieve()
                .bodyToMono(ExchangeData.class)
                .doOnError(ex -> log.error("Can't connect with {} for read exchange={}", baseUrl, exchangeName, ex))
                .doOnSuccess(v -> log.info("Read exchange {} exchangeName={}", baseUrl, exchangeName))
                .onErrorResume(this::emptyOnClientErrorMono);
    }

    public Mono<Void> createExchange(String exchangeName, CreateExchangeRequest createExchangeRequest) {

        return client()
                .put()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/exchanges/{vhost}/{exchangeName}")
                        .build("/", exchangeName))
                .bodyValue(createExchangeRequest)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(ex -> log.error("Can't create exchange {} exchangeName={}", baseUrl, exchangeName, ex))
                .doOnSuccess(v -> log.info("Created exchange {} exchangeName={}", baseUrl, exchangeName))
                .onErrorResume(this::emptyOnClientErrorMono);
    }

    public Mono<Void> deleteQueue(String queueName) {

        return client()
                .delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/queues/{vhost}/{queueName}")
                        .build("/", queueName))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(ex -> log.error("Can't delete queue {} queueName={}", baseUrl, queueName, ex))
                .doOnSuccess(v -> log.info("Deleted queue {} queueName={}", baseUrl, queueName))
                .onErrorResume(this::emptyOnClientErrorMono);

    }

    public Mono<Void> createBinding(String exchangeName, String detinationQueue, CreateBindingRequest bindingRequest) {

        return client()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/bindings/{vhost}/e/{exchangeName}/q/{destinationQueue}")
                        .build("/", exchangeName, detinationQueue))
                .bodyValue(bindingRequest)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(ex -> log.error("Can't create binding {} exchangeName={} destinationQueue={}",
                        baseUrl, exchangeName, detinationQueue, ex))
                .onErrorResume(this::emptyOnClientErrorMono);
    }

    private boolean isClientError(Throwable ex) {

        if (ex instanceof WebClientResponseException wcre) {
            return wcre.getStatusCode().value() == 404;
        }

        return Optional.ofNullable(ex.getCause())
                .map(c -> c instanceof ConnectException)
                .orElse(false);
    }

    private <T> Mono<T> emptyOnClientErrorMono(Throwable ex) {
        return isClientError(ex) ? Mono.empty() : Mono.error(ex);
    }

    private <T> Flux<T> emptyOnClientErrorFlux(Throwable ex) {
        return isClientError(ex) ? Flux.empty() : Flux.error(ex);
    }

}
