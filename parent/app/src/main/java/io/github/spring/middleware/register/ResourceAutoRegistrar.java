package io.github.spring.middleware.register;

import io.github.spring.middleware.annotations.Register;
import io.github.spring.middleware.client.proxy.ProxyClientRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
public class ResourceAutoRegistrar implements ApplicationListener<ApplicationReadyEvent> {

    private final ResourceRegister resourceRegister;

    public ResourceAutoRegistrar(ResourceRegister resourceRegister) {
        this.resourceRegister = resourceRegister;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Only act on root context to avoid double execution in child contexts
        if (event.getApplicationContext().getParent() != null) {
            return;
        }

        // At this point the application context is ready and the embedded server has started.
        // Still wait briefly for proxy configuration if needed.
        int tries = 0;
        while (ProxyClientRegistry.getAll().isEmpty() && tries < 10) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            tries++;
        }

        // Scan beans annotated with @Register
        Set<Class<?>> resourceClasses = event.getApplicationContext().getBeansWithAnnotation(Register.class)
                .values().stream()
                .map(Object::getClass)
                .collect(Collectors.toSet());

        if (!resourceClasses.isEmpty()) {
            resourceRegister.register(resourceClasses);
            resourceClasses.forEach(c -> log.info("Discovered and registered resource {}", c.getSimpleName()));
        } else {
            log.info("No resources annotated with @Register were found to register");
        }
    }
}
