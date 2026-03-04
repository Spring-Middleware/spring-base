package io.github.spring.middleware.register.resource;

import io.github.spring.middleware.annotation.Register;
import io.github.spring.middleware.client.RegistryClient;
import io.github.spring.middleware.registry.model.RegistryEntry;
import io.github.spring.middleware.registry.model.RegistryMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Component
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
public class ResourceAutoRegistrar implements ApplicationListener<ApplicationReadyEvent> {

    private final ResourceRegister resourceRegister;
    private Set<Class<?>> resourcesToRegister = Set.of();
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final RegistryClient registryClient;

    public ResourceAutoRegistrar(final ResourceRegister resourceRegister, final RegistryClient registryClient) {
        this.resourceRegister = resourceRegister;
        this.registryClient = registryClient;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Only act on root context to avoid double execution in child contexts
        if (event.getApplicationContext().getParent() != null) return;
        if (!initialized.compareAndSet(false, true)) return;

        // Scan beans annotated with @Register
        this.resourcesToRegister = event.getApplicationContext().getBeansWithAnnotation(Register.class)
                .values().stream()
                .map(bean -> AopUtils.getTargetClass(bean))
                .collect(Collectors.toSet());

        if (!this.resourcesToRegister.isEmpty()) {
            resourceRegister.register(this.resourcesToRegister);
            this.resourcesToRegister.forEach(c -> log.info("Discovered and registered resource {}", c.getSimpleName()));
        } else {
            log.info("No resources annotated with @Register were found to register");
        }
    }

    public Set<Class<?>> getResourcesToRegister() {
        return resourcesToRegister;
    }

    public void registerResourcesNotRegistered() {
        final Map<String, RegistryEntry> registries;
        final RegistryMap currentRegistryMap = registryClient.getRegistryMap();
        try {
            registries = currentRegistryMap.registryMap();
        } catch (Exception e) {
            log.warn("Failed to load registry entries", e);
            return;
        }

        Set<Class<?>> notRegistered = this.resourcesToRegister.stream()
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
