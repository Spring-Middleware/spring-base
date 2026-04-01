package io.github.spring.middleware.graphql.gateway.runtime;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class GraphQLBatchingToggle {

    private final AtomicBoolean enabled = new AtomicBoolean(true);

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }
}
