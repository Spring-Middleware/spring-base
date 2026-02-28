package io.github.spring.middleware.client.config;

import io.github.spring.middleware.client.proxy.ProxyClientRegistry;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProxyClientManager implements ApplicationListener<ContextRefreshedEvent> {

    private final ProxyClientResilienceConfigurator clientConfigurator;
    private volatile boolean configured = false;

    public ProxyClientManager(final ProxyClientResilienceConfigurator clientConfigurator) {
        this.clientConfigurator = clientConfigurator;
    }

    public void configureAll() {
        clientConfigurator.configureProxies(ProxyClientRegistry.getAll());
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
