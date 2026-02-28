package io.github.spring.middleware.client.config;

import io.github.spring.middleware.annotations.MiddlewareClient;
import io.github.spring.middleware.client.RegistryClient;
import io.github.spring.middleware.client.proxy.ProxyClient;
import io.github.spring.middleware.registry.model.RegistryEntry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ProxyClientConfigurationTask implements Runnable {

    private final ProxyClient<?> proxyClient;
    private final RegistryClient registryClient;
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final ProxyClientConfigurationProperties proxyClientConfigurationProperties;

    public ProxyClientConfigurationTask(ProxyClient<?> proxyClient, RegistryClient registryClient, ProxyClientConfigurationProperties properties) {
        this.proxyClient = proxyClient;
        this.registryClient = registryClient;
        this.proxyClientConfigurationProperties = properties;
    }

    /**
     * Inicia la tarea de configuración del proxy de manera reactiva y no bloqueante.
     */
    public void run() {
        MiddlewareClient clientAnnotation = proxyClient.getInterf().getAnnotation(MiddlewareClient.class);

        attemptConfigure(clientAnnotation)
                .repeatWhenEmpty(repeat -> repeat.delayElements(Duration.ofSeconds(10)))
                .takeUntilOther(Mono.fromRunnable(() -> stop.set(true)))
                .subscribe(
                        entry -> log.info("Configured proxy client {} -> {}", proxyClient.getInterf().getSimpleName(), entry.getClusterEndpoint()),
                        ex -> log.error("Failed to configure proxy client {}", proxyClient.getInterf().getSimpleName(), ex),
                        () -> log.warn("Configuration task stopped for {}", proxyClient.getInterf().getSimpleName())
                );
    }

    private Mono<RegistryEntry> attemptConfigure(MiddlewareClient clientAnnotation) {
        if (stop.get()) {
            return Mono.empty();
        }

        return Mono.fromCallable(() -> {
                    RegistryEntry registryEntry = registryClient.getRegistryEntry(clientAnnotation.value());
                    proxyClient.setRegistryEntry(registryEntry);
                    proxyClient.setProxyClientConfigurationProperties(proxyClientConfigurationProperties);
                    proxyClient.configureHttpClient();

                    if (registryEntry == null) {
                        log.warn("Can't configure proxy client {}: {} not found in registry",
                                proxyClient.getInterf().getSimpleName(), clientAnnotation.value());
                    }

                    return registryEntry;
                })
                .onErrorResume(ex -> {
                    log.error("Error connecting to registry for {}", proxyClient.getInterf().getSimpleName(), ex);
                    return Mono.empty();
                });
    }

    public void stop() {
        stop.set(true);
    }

    public ProxyClient<?> getProxyClient() {
        return proxyClient;
    }
}