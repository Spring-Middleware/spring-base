package io.github.spring.middleware.client.proxy;

import io.github.spring.middleware.client.config.ProxyClientManager;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupProxyConfigurator {

    private final ProxyClientManager proxyClientManager;

    public StartupProxyConfigurator(ProxyClientManager proxyClientManager) {
        this.proxyClientManager = proxyClientManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void configureProxiesOnStartup() {
        proxyClientManager.configureAll();
    }
}
