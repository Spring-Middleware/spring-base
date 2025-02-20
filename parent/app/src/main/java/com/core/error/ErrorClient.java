package com.core.error;

import com.core.error.api.ErrorRecoveryAttemptRequest;
import com.core.error.api.ErrorRecoveryAttemptView;
import com.core.error.api.ErrorSearch;
import com.core.error.api.ErrorView;
import com.core.http.AbstractWebClient;
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

    public Flux<ErrorView> searchErrors(ErrorSearch errorSearch) throws Exception {

        Flux errorFlux = null;
        if (webClient != null) {
            errorFlux = webClient.post().uri("/error/search")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(Mono.just(errorSearch), ErrorSearch.class).retrieve().bodyToFlux(ErrorView.class)
                    .doOnError(ex -> log.error("Can't connect with " + baseUrl))
                    .onErrorResume(ex -> {
                        if (Optional.ofNullable(ex.getCause()).map(e -> e instanceof ConnectException)
                                .orElse(Boolean.FALSE)) {
                            return Flux.empty();
                        } else {
                            return Mono.error(ex);
                        }
                    });

        } else {
            log.warn("ErrorClient not init");
        }
        return errorFlux;
    }

    public Mono<ErrorRecoveryAttemptView> setErrorRecoveryAttempt(UUID errorId,
            ErrorRecoveryAttemptRequest errorRecoveryAttemptRequest) throws Exception {

        if (webClient != null) {
            return webClient.post().uri("/errorRecoveryAttempt/" + errorId)
                    .body(Mono.just(errorRecoveryAttemptRequest), ErrorRecoveryAttemptRequest.class).retrieve()
                    .bodyToMono(ErrorRecoveryAttemptView.class);
        } else {
            throw new Exception("WebClient not init");
        }
    }

}
