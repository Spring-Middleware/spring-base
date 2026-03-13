package io.github.spring.middleware.scheduler;

import io.github.spring.middleware.annotation.Register;
import io.github.spring.middleware.client.RegistryClient;
import io.github.spring.middleware.manager.ProxyConfigurationClientManager;
import io.github.spring.middleware.manager.RegistrationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "middleware.registry-consistency-scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class RegistryConsistencyScheduler {

    private final RegistryChecker registryChecker;
    private final RegistrationManager registrationManager;
    private final ProxyConfigurationClientManager proxyConfigurationClientManager;

    private final AtomicInteger consecutiveRegistryFailures = new AtomicInteger(0);
    private final AtomicBoolean degraded = new AtomicBoolean(false);

    // Cuando el registry no responde
    private static final int MAX_REGISTRY_FAILS = 2;

    // Cuando el registry responde pero mi nodo aún no está en schemaLocationNodes
    private final AtomicInteger consecutiveNodeNotRegistered = new AtomicInteger(0);
    private static final int MAX_NODE_NOT_REGISTERED = 2;

    @Scheduled(cron = "${middleware.registry-consistency-scheduler.cron:*/30 * * * * *}")
    public void tick() {
        log.info("Checking registry consistency (clients/resources/schemas)");
        // 1) Registry alive?
        if (!isRegistryAlive()) {
            int fails = consecutiveRegistryFailures.incrementAndGet();
            log.warn("Registry not reachable (fails={})", fails);

            if (fails >= MAX_REGISTRY_FAILS && degraded.compareAndSet(false, true)) {
                log.warn("Entering degraded mode: stopping proxy configuration tasks (registry DOWN)");
                proxyConfigurationClientManager.stopAll();
            }
            return;
        }

        // registry OK
        consecutiveRegistryFailures.set(0);
        if (degraded.compareAndSet(true, false)) {
            log.info("Registry is back. Leaving degraded mode.");
        }

        // 2) Proxies
        log.debug("Configuring proxies not bounded");
        proxyConfigurationClientManager.configureNotBounded();

        // 3) Schemas
        boolean hasSchemas = registrationManager.hasSchemasToRegister();
        log.info("Schema check: hasSchemasToRegister={}, consecutiveNodeNotRegistered={}",
                hasSchemas, consecutiveNodeNotRegistered.get());

        if (hasSchemas) {
            checkRegisterSchemas();
        } else {
            log.info("Schema check skipped: hasSchemasToRegister=false");
        }

        boolean hasResources = registrationManager.hasResourcesToRegister();
        log.info("Resource check: hasResourcesToRegister={}", hasResources);

        if (hasResources) {
            checkRegisterEndpoints();
        } else {
            log.info("Resource check skipped: hasResourcesToRegister=false");
        }
    }

    private void checkRegisterEndpoints() {
        log.debug("Checking registry endpoints consistency");

        var resourcesMissingEndpoint = registrationManager.getResourcesToRegister().stream()
                .filter(clazz -> {
                    Register register = clazz.getAnnotation(Register.class);
                    return !registrationManager.isEndpointRegistered(register);
                })
                .collect(Collectors.toSet());

        if (resourcesMissingEndpoint.isEmpty()) {
            log.debug("All resource endpoints are registered");
            return;
        }

        log.info("Found {} resource endpoints not registered", resourcesMissingEndpoint.size());
        registrationManager.registerResources(resourcesMissingEndpoint);
    }


    private void checkRegisterSchemas() {
        log.debug("Checking registry schemas consistency");
        boolean nodeRegistered;
        try {
            nodeRegistered = registrationManager.isSchemaNodeRegistered();
        } catch (Exception ex) {
            log.warn("Could not determine if schema node is registered; treating as NOT registered", ex);
            nodeRegistered = false;
        }

        log.info("Schema check: nodeRegistered={}", nodeRegistered);

        if (!nodeRegistered) {
            int n = consecutiveNodeNotRegistered.incrementAndGet();
            log.warn("Schema node is NOT registered yet (fails={}/{})", n, MAX_NODE_NOT_REGISTERED);

            if (n >= MAX_NODE_NOT_REGISTERED) {
                log.info("Attempting to register resources + schemas (node was not registered)");
                try {
                    registrationManager.registerEverything();
                    log.info("registerEverything executed");
                } catch (Exception ex) {
                    log.warn("Error registering everything", ex);
                    proxyConfigurationClientManager.stopAll();
                    return;
                }
            } else {
                log.info("Node not yet stable, stopping proxy configuration tasks");
                proxyConfigurationClientManager.stopAll();
            }
            return;
        }

        log.info("Schema node already registered. Resetting counter.");
        consecutiveNodeNotRegistered.set(0);
    }


    private boolean isRegistryAlive() {
        try {
            Boolean alive = registryChecker.isAlive()
                    .map(m -> {
                        log.debug("Registry alive check result: {}", m);
                        Object status = m.get("status");
                        return status != null && "UP".equalsIgnoreCase(status.toString().trim());
                    })
                    .onErrorReturn(false)
                    .block();

            return Boolean.TRUE.equals(alive);
        } catch (Exception e) {
            log.debug("isRegistryAlive check failed", e);
            return false;
        }
    }
}