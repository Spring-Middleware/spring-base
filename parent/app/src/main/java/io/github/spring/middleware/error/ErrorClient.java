package io.github.spring.middleware.error;

import io.github.spring.middleware.error.api.ErrorRecoveryAttemptRequest;
import io.github.spring.middleware.error.api.ErrorRecoveryAttemptView;
import io.github.spring.middleware.error.api.ErrorSearch;
import io.github.spring.middleware.error.api.ErrorView;
import io.github.spring.middleware.http.AbstractWebClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class ErrorClient extends AbstractWebClient {

    @Value("${com.error-handler.baseUrl:#{null}}")
    private String baseUrl;

    @Override
    protected String getBaseUrl() {

        return baseUrl;
    }

    public Flux<ErrorView> searchErrors(ErrorSearch errorSearch) {

        return client().post()
                .uri("/error/search")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(errorSearch)
                .retrieve()
                .bodyToFlux(ErrorView.class)
                .doOnError(ex -> log.error("Can't connect with {}", baseUrl, ex))
                .onErrorResume(ex ->
                        Optional.ofNullable(ex.getCause()).filter(c -> c instanceof ConnectException).isPresent()
                                ? Flux.empty()
                                : Flux.error(ex)
                );
    }

    public Mono<ErrorRecoveryAttemptView> setErrorRecoveryAttempt(UUID errorId,
                                                                  ErrorRecoveryAttemptRequest errorRecoveryAttemptRequest) {

        return client().post()
                .uri("/errorRecoveryAttempt/{id}", errorId)
                .bodyValue(errorRecoveryAttemptRequest)
                .retrieve()
                .bodyToMono(ErrorRecoveryAttemptView.class);
    }

}
