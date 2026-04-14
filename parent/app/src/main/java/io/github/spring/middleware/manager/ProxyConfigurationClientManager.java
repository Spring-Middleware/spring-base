package io.github.spring.middleware.manager;

import io.github.spring.middleware.client.config.ProxyClientResilienceConfigurator;
import io.github.spring.middleware.client.proxy.ProxyClient;
import io.github.spring.middleware.client.proxy.ProxyClientRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProxyConfigurationClientManager implements ApplicationListener<ContextRefreshedEvent> {

    private final ProxyClientResilienceConfigurator clientConfigurator;
    private volatile boolean configured = false;

    public ProxyConfigurationClientManager(final ProxyClientResilienceConfigurator clientConfigurator) {
        this.clientConfigurator = clientConfigurator;
    }

    public void configureAll() {
        this.clientConfigurator.configureProxies(ProxyClientRegistry.getAll());
    }

    public void configureNotBounded() {
        Set<ProxyClient<?>> unconfigured = ProxyClientRegistry.getAll().stream()
                .filter(p -> p.getRegistryEntry() == null)
                .collect(Collectors.toSet());
        if (!unconfigured.isEmpty()) {
            log.debug("Configuring {} unconfigured proxy clients", unconfigured.size());
            clientConfigurator.configureProxies(unconfigured);
        }
    }

    public void stopAll() {
        this.clientConfigurator.stopAll();
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Ensure configureAll runs only once and as early ordered listener
        if (!configured) {
            synchronized (this) {
                if (!configured) {
                    configureAll();
                    configured = true;
                }
            }
        }
    }
}
