package io.github.spring.middleware.client.proxy;

import io.github.spring.middleware.client.config.ProxyClientsReadyEvent;
import io.github.spring.middleware.manager.ProxyConfigurationClientManager;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupProxyConfigurator {

    private final ProxyConfigurationClientManager proxyConfigurationClientManager;
    private final ApplicationEventPublisher eventPublisher;

    public StartupProxyConfigurator(ProxyConfigurationClientManager proxyConfigurationClientManager, ApplicationEventPublisher eventPublisher) {
        this.proxyConfigurationClientManager = proxyConfigurationClientManager;
        this.eventPublisher = eventPublisher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void configureProxiesOnStartup() {
        proxyConfigurationClientManager.configureAll();
        eventPublisher.publishEvent(new ProxyClientsReadyEvent());
    }
}
