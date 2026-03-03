package io.github.spring.middleware.scheduler;

import io.github.spring.middleware.annotation.Register;
import io.github.spring.middleware.client.RegistryClient;
import io.github.spring.middleware.client.config.ProxyClientResilienceConfigurator;
import io.github.spring.middleware.client.proxy.ProxyClient;
import io.github.spring.middleware.client.proxy.ProxyClientRegistry;
import io.github.spring.middleware.register.resource.ResourceRegister;
import io.github.spring.middleware.registry.model.RegistryEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static io.github.spring.middleware.provider.ApplicationContextProvider.getApplicationContext;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "middleware.registry-consistency-scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class RegistryConsistencyScheduler {

    private final RegistryClient registryClient;
    private final ProxyClientResilienceConfigurator clientConfigurator;
    private final ResourceRegister resourceRegister;

    private final AtomicInteger consecutiveRegistryFailures = new AtomicInteger(0);
    private final AtomicBoolean degraded = new AtomicBoolean(false);
    private static final int MAX_FAILS = 2;

    @Scheduled(cron = "*/30 * * * * *") // cada 30 segundos
    public void tick() {
        log.info("Checking registry consistency (clients/resources)");

        if (!isRegistryAlive()) {
            int fails = consecutiveRegistryFailures.incrementAndGet();
            log.warn("Registry not reachable (fails={})", fails);

            if (fails >= MAX_FAILS && degraded.compareAndSet(false, true)) {
                log.warn("Entering degraded mode: stopping proxy configuration tasks");
                clientConfigurator.stopAll();
            }
            return;
        }

        // registry OK
        consecutiveRegistryFailures.set(0);
        if (degraded.compareAndSet(true, false)) {
            log.info("Registry is back. Leaving degraded mode.");
        }

        checkClients();
        checkResources();
    }

    private boolean isRegistryAlive() {
        try {
            Map<String, String> resp = registryClient.isAlive(); // timeout dentro del client
            if (resp == null) return false;
            String status = resp.get("status");
            return status != null && "UP".equalsIgnoreCase(status.trim());
        } catch (Exception e) {
            log.debug("isRegistryAlive check failed", e);
            return false;
        }
    }

    private void checkClients() {
        Set<ProxyClient<?>> unconfigured = ProxyClientRegistry.getAll().stream()
                .filter(p -> p.getRegistryEntry() == null)
                .collect(Collectors.toSet());

        if (!unconfigured.isEmpty()) {
            log.info("Configuring {} unconfigured proxy clients", unconfigured.size());
            clientConfigurator.configureProxies(unconfigured);
        }
    }

    private void checkResources() {
        final Map<String, RegistryEntry> registries;
        try {
            registries = registryClient.getRegistryMap().registryMap();
        } catch (Exception e) {
            log.warn("Failed to load registry entries", e);
            return;
        }

        // Resolve target classes (un-proxy) and then resolve @Register safely
        Set<Class<?>> resourcesToCheck = getApplicationContext()
                .getBeansWithAnnotation(Register.class)
                .values().stream()
                .map(AopUtils::getTargetClass)
                .collect(Collectors.toSet());

        Set<Class<?>> notRegistered = resourcesToCheck.stream()
                .filter(clazz -> {
                    Register ann = AnnotationUtils.findAnnotation(clazz, Register.class);
                    if (ann == null) return false; // safety
                    return !registries.containsKey(ann.name());
                })
                .collect(Collectors.toSet());

        if (!notRegistered.isEmpty()) {
            String names = notRegistered.stream()
                    .map(c -> {
                        Register ann = AnnotationUtils.findAnnotation(c, Register.class);
                        return ann != null ? ann.name() : c.getSimpleName();
                    })
                    .collect(Collectors.joining(", "));

            log.info("Registering {} resources not found in registry: {}", notRegistered.size(), names);
            resourceRegister.register(notRegistered);
        }
    }
}