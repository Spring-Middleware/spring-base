package io.github.spring.middleware.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.spring.middleware.annotation.MiddlewareContract;
import io.github.spring.middleware.client.RegistryClient;
import io.github.spring.middleware.client.proxy.*;
import io.github.spring.middleware.registry.model.RegistryEntry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ProxyClientConfigurationTask implements Runnable {

    private final ProxyClient<?> proxyClient;
    private final RegistryClient registryClient;
    private final MiddlewareClientConfigParameters clientConfigParameters;
    private final ProxyClientConfigurationTaskConfigurationProperties taskConfigProperties;
    private final ProxyConnectionErrorHandler errorHandler;
    private final ProxyClientAnalyzer proxyClientAnalyzer;
    private final ClusterBulkheadRegistry clusterBulkheadRegistry;
    private final ClusterCircuitBreakerRegistry clusterCircuitBreakerRegistry;
    private final ObjectMapper objectMapper;

    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final Sinks.One<Void> stopSink = Sinks.one();
    private volatile Disposable subscription;


    public ProxyClientConfigurationTask(
            final ProxyClient<?> proxyClient,
            final RegistryClient registryClient,
            final MiddlewareClientConfigParameters clientConfigParameters,
            final ProxyClientConfigurationTaskConfigurationProperties taskConfigProperties,
            final ProxyConnectionErrorHandler proxyConnectionErrorHandler,
            final ProxyClientAnalyzer proxyClientAnalyzer,
            final ClusterBulkheadRegistry clusterBulkheadRegistry,
            final ClusterCircuitBreakerRegistry clusterCircuitBreakerRegistry,
            final ObjectMapper objectMapper
    ) {
        this.proxyClient = Objects.requireNonNull(proxyClient, "proxyClient");
        this.registryClient = Objects.requireNonNull(registryClient, "registryClient");
        this.clientConfigParameters = Objects.requireNonNull(clientConfigParameters, "clientConfigParameters");
        this.taskConfigProperties = Objects.requireNonNull(taskConfigProperties, "taskConfigProperties");
        this.errorHandler = Objects.requireNonNull(proxyConnectionErrorHandler, "proxyConnectionErrorHandler");
        this.proxyClientAnalyzer = Objects.requireNonNull(proxyClientAnalyzer, "proxyClientAnalyzer");
        this.clusterBulkheadRegistry = Objects.requireNonNull(clusterBulkheadRegistry, "clusterBulkheadRegistry");
        this.clusterCircuitBreakerRegistry = Objects.requireNonNull(clusterCircuitBreakerRegistry, "clusterCircuitBreakerRegistry");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public void run() {
        // Evita doble arranque
        if (this.subscription != null && !this.subscription.isDisposed()) {
            log.debug("Configuration task already running for {}", proxyClient.getInterf().getSimpleName());
            return;
        }

        MiddlewareContract contract = proxyClient.getInterf().getAnnotation(MiddlewareContract.class);
        if (contract == null || contract.name() == null || contract.name().isBlank()) {
            log.warn("Proxy client {} has no @MiddlewareContract(value). Skipping configuration.",
                    proxyClient.getInterf().getSimpleName());
            return;
        }

        String endpointName = contract.name().trim();

        this.subscription =
                attemptConfigure(endpointName)
                        // Si no existe en registry => Mono.empty() => reintenta cada 10s
                        .repeatWhenEmpty(r -> r.delayElements(Duration.ofSeconds(taskConfigProperties.getRetryDelaySeconds())))
                        // Si hay error (fallo red/registry) => reintenta con backoff
                        .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(taskConfigProperties.getRetryMinBackoffSeconds()))
                                .maxBackoff(Duration.ofSeconds(taskConfigProperties.getRetryMaxBackoffSeconds()))
                                .jitter(0.3)
                                .doBeforeRetry(rs -> log.warn(
                                        "Retrying registry lookup for {} (attempt={}, reason={})",
                                        proxyClient.getInterf().getSimpleName(),
                                        rs.totalRetries() + 1,
                                        rs.failure() != null ? rs.failure().getMessage() : "unknown"
                                )))
                        // Parada cooperativa
                        .takeUntilOther(stopSink.asMono())
                        .subscribe(
                                entry -> log.info("Configured proxy client {} -> {}",
                                        proxyClient.getInterf().getSimpleName(),
                                        entry.getClusterEndpoint()),
                                ex -> log.error("Configuration task failed for {}",
                                        proxyClient.getInterf().getSimpleName(), ex),
                                () -> log.info("Configuration task stopped for {}",
                                        proxyClient.getInterf().getSimpleName())
                        );
    }

    private Mono<RegistryEntry> attemptConfigure(String endpointName) {
        if (stopped.get()) {
            return Mono.empty();
        }

        // registryClient.getRegistryEntry(...) parece bloqueante -> boundedElastic
        return Mono.fromCallable(() -> registryClient.getRegistryEntry(endpointName))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(entry -> {
                    if (stopped.get()) return Mono.empty();

                    if (entry == null) {
                        log.warn("Can't configure proxy client {}: '{}' not found in registry",
                                proxyClient.getInterf().getSimpleName(), endpointName);
                        return Mono.empty(); // dispara repeatWhenEmpty
                    }

                    // Configura SOLO cuando hay entry
                    proxyClient.setRegistryEntry(entry);
                    proxyClient.setMiddlewareClientConfigParameters(clientConfigParameters);
                    proxyClient.setBulkheadRegistry(clusterBulkheadRegistry);
                    proxyClient.setCircuitBreakerRegistry(clusterCircuitBreakerRegistry);
                    proxyClient.setErrorHandler(errorHandler);
                    proxyClient.setObjectMapper(objectMapper);
                    proxyClient.setMethodMethodMetaDataMap(proxyClientAnalyzer.analyze(proxyClient.getInterf()));
                    proxyClient.configureHttpClient();

                    return Mono.just(entry);
                });
    }

    public void stop() {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }

        // Señal de parada para cortar repeat/retry
        stopSink.tryEmitEmpty();

        // Cancela la suscripción si existe
        Disposable sub = this.subscription;
        if (sub != null && !sub.isDisposed()) {
            sub.dispose();
        }
    }

    public ProxyClient<?> getProxyClient() {
        return proxyClient;
    }
}