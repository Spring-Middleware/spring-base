package io.github.spring.middleware.registry.scanner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static io.github.spring.middleware.utils.EndpointUtils.joinUrl;
import static io.github.spring.middleware.utils.EndpointUtils.normalizePath;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegistryScannerClient {

    private final WebClient webClient;
    private final RegistryTopologyReconcilerProperties props;

    public Mono<Boolean> isAlive(String nodeLocation) {
        String uri = joinUrl(STR."http://\{nodeLocation}", normalizePath(props.getHealthPath()));

        return webClient.get()
                .uri(uri)
                .exchangeToMono(resp ->
                        resp.bodyToMono(String.class)
                                .map(body -> resp.statusCode().is2xxSuccessful() && body.contains("\"status\":\"UP\""))
                )
                .timeout(Duration.ofMillis(props.getTimeoutMillis()))
                .doOnNext(alive -> log.debug("Health {} -> {}", uri, alive))
                .onErrorReturn(false);
    }

    public Mono<Void> triggerRegisterResource(String nodeLocation) {
        String uri = joinUrl(STR."http://\{nodeLocation}", normalizePath(props.getRegisterResourcePath()));

        return webClient.get()
                .uri(uri)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofMillis(props.getTimeoutMillis()))
                .doOnSuccess(r -> log.info("Sent registerResource trigger to {}", nodeLocation))
                .doOnError(ex -> log.warn("Error sending registerResource to {}", nodeLocation, ex))
                .then();
    }
}
