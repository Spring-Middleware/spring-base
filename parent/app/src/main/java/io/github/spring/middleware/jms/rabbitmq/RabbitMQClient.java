package io.github.spring.middleware.jms.rabbitmq;

import io.github.spring.middleware.config.JmsConfiguration;
import io.github.spring.middleware.http.AbstractWebClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.Optional;

@Slf4j
@Component
public class RabbitMQClient extends AbstractWebClient {

    @Value("${jms.rabbitmq.baseUrl:#{null}}")
    private String baseUrl;

    @Autowired
    private JmsConfiguration jmsConfiguration;

    @Override
    protected String getBaseUrl() {

        return baseUrl;
    }

    public Flux<RabbitConsumerData> getConsumers() {

        Flux flux = null;
        if (webClient != null) {
            flux = webClient.get().uri("/api/consumers").retrieve().bodyToFlux(RabbitConsumerData.class)
                    .doOnError(ex -> log.error("Can't connect with " + baseUrl))
                    .onErrorResume(ex -> {
                        if (Optional.ofNullable(ex.getCause()).map(e -> e instanceof ConnectException)
                                .orElse(Boolean.FALSE)) {
                            return Flux.empty();
                        } else {
                            return Mono.error(ex);
                        }
                    });
        }
        return flux;
    }

    public Flux<RabbitBindingData> getBindingsForExchange(String exchangeName) {

        Flux flux = null;
        if (webClient != null) {
            flux = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/exchanges/{vhost}/{exchangeName}/bindings/source")
                            .build('/', exchangeName)).retrieve()
                    .bodyToFlux(RabbitBindingData.class).doOnError(ex -> log.error(
                            "Can't connect with " + baseUrl + " for read binding for exchange " + exchangeName))
                    .onErrorResume(ex -> {
                        if (Optional.ofNullable(ex.getCause()).map(e -> e instanceof ConnectException)
                                .orElse(Boolean.FALSE)) {
                            return Flux.empty();
                        } else {
                            return Mono.error(ex);
                        }
                    });
        }
        return flux;
    }

    public Mono<ExchangeData> getExchange(String exchangeName) {

        Mono<ExchangeData> exchange = null;
        if (webClient != null) {
            exchange = this.webClient.get()
                    .uri(uriBuilder ->
                            uriBuilder.path("/api/exchanges/{vhost}/{exchangeName}")
                                    .build('/', exchangeName))
                    .retrieve()
                    .bodyToMono(ExchangeData.class)
                    .doOnError(ex -> log.error("Can't connect with " + baseUrl + " for read exchange " + exchangeName))
                    .onErrorResume(ex -> {
                        if (Optional.ofNullable(ex.getCause()).map(e -> e instanceof ConnectException)
                                .orElse(Boolean.FALSE)) {
                            return Mono.empty();
                        } else {
                            return Mono.error(ex);
                        }
                    });
        }
        return exchange;
    }

    public Mono<Void> createExchange(String exchangeName, CreateExchangeRequest createExchangeRequest) {

        Mono<Void> result = null;
        if (webClient != null) {
            result = this.webClient.put().uri(uriBuilder ->
                            uriBuilder.path("/api/exchanges/{vhost}/{exchangeName}")
                                    .build('/', exchangeName))
                    .body(Mono.just(createExchangeRequest), CreateExchangeRequest.class)
                    .retrieve().bodyToMono(Void.class).doOnError(ex -> log.error(
                            "Can't create exchnage" + baseUrl + " exchangeName=" + exchangeName))
                    .onErrorResume(ex -> {
                        if (Optional.ofNullable(ex.getCause()).map(e -> e instanceof ConnectException)
                                .orElse(Boolean.FALSE)) {
                            return Mono.empty();
                        } else {
                            return Mono.error(ex);
                        }
                    });
        }
        return result;
    }

    public Mono<Void> createBinding(String exchangeName, String detinationQueue, CreateBindingRequest bindingRequest) {

        Mono<Void> result = null;
        if (webClient != null) {
            result = this.webClient.post().uri(uriBuilder ->
                            uriBuilder.path("/api/bindings/{vhost}/e/{exchangeName}/q/{destinationQueue}")
                                    .build('/', exchangeName, detinationQueue))
                    .body(Mono.just(bindingRequest), CreateBindingRequest.class)
                    .retrieve().bodyToMono(Void.class).doOnError(ex -> log.error(
                            "Can't create binding " + baseUrl + " exchangeName=" + exchangeName + " destinationQueue=" +
                                    detinationQueue))
                    .onErrorResume(ex -> {
                        if (Optional.ofNullable(ex.getCause()).map(e -> e instanceof ConnectException)
                                .orElse(Boolean.FALSE)) {
                            return Mono.empty();
                        } else {
                            return Mono.error(ex);
                        }
                    });

        }
        return result;
    }

    protected ExchangeFilterFunction getExchangeFilterFunction() {

        return ExchangeFilterFunctions.basicAuthentication(jmsConfiguration.getUser(), jmsConfiguration.getPassword());
    }

}
