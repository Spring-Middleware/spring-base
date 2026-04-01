package io.github.spring.middleware.graphql.gateway.config;

import io.github.spring.middleware.graphql.gateway.runtime.GraphQLBatchingToggle;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@RequiredArgsConstructor
@ConfigurationProperties("middleware.graphql.gateway.batching")
public class GraphQLBatchingProperties {

    private final GraphQLBatchingToggle toggle;

    private boolean enabled = false;

    @PostConstruct
    public void init() {
        toggle.setEnabled(enabled);
    }

}
