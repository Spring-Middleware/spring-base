package io.github.spring.middleware.scheduler;

import io.github.spring.middleware.http.AbstractWebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class RegistryChecker extends AbstractWebClient {

    @Value("${middleware.client.registry-endpoint}")
    private String registryEndpoint;

    @Override
    protected String getBaseUrl() {
        return registryEndpoint;
    }

    public Mono<Map<String, Object>> isAlive() {
        return client().get()
                .uri("/_alive")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
