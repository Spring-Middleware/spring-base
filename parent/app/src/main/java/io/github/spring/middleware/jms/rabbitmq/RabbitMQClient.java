package io.github.spring.middleware.jms.rabbitmq;

import io.github.spring.middleware.config.JmsConfiguration;
import io.github.spring.middleware.http.AbstractWebClient;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "jms.rabbitmq.baseUrl")
public class RabbitMQClient extends AbstractWebClient {

    @Value("${jms.rabbitmq.baseUrl:#{null}}")
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

    public Flux<RabbitConsumerData> getConsumers() {

        return client()
                .get()
                .uri("/api/consumers")
                .retrieve()
                .bodyToFlux(RabbitConsumerData.class)
                .doOnError(ex -> log.error("Can't connect with {}", baseUrl, ex))
                .onErrorResume(this::emptyOnConnectErrorFlux);
    }

    public Flux<RabbitBindingData> getBindingsForExchange(String exchangeName) {

        return client()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/exchanges/{vhost}/{exchangeName}/bindings/source")
                        .build("/", exchangeName))
                .retrieve()
                .bodyToFlux(RabbitBindingData.class)
                .doOnError(ex -> log.error("Can't connect with {} for read binding exchange={}", baseUrl, exchangeName, ex))
                .onErrorResume(this::emptyOnConnectErrorFlux);
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
                .onErrorResume(this::emptyOnConnectErrorMono);
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
                .onErrorResume(this::emptyOnConnectErrorMono);
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
                .onErrorResume(this::emptyOnConnectErrorMono);
    }

    private boolean isConnectError(Throwable ex) {
        return Optional.ofNullable(ex.getCause()).map(c -> c instanceof ConnectException).orElse(false);
    }

    private <T> Mono<T> emptyOnConnectErrorMono(Throwable ex) {
        return isConnectError(ex) ? Mono.empty() : Mono.error(ex);
    }

    private <T> Flux<T> emptyOnConnectErrorFlux(Throwable ex) {
        return isConnectError(ex) ? Flux.empty() : Flux.error(ex);
    }

}
