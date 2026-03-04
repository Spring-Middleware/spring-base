package io.github.spring.middleware.scheduler;

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

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "middleware.registry-consistency-scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class RegistryConsistencyScheduler {

    private final RegistryClient registryClient;
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

        } else {
            log.info("Schema check skipped: hasSchemasToRegister=false");
        }

        // 4) Resources self-heal
        log.debug("Checking resources not registered");
        registrationManager.registerResourcesNotRegistered();
    }

    private boolean isRegistryAlive() {
        try {
            Map<String, String> resp = registryClient.isAlive();
            if (resp == null) return false;
            String status = resp.get("status");
            return status != null && "UP".equalsIgnoreCase(status.trim());
        } catch (Exception e) {
            log.debug("isRegistryAlive check failed", e);
            return false;
        }
    }
}