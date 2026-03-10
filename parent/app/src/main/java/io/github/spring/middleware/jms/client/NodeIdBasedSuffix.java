package io.github.spring.middleware.jms.client;

import io.github.spring.middleware.component.NodeInfoRetriever;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationSuffix;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NodeIdBasedSuffix implements DestinationSuffix {

    private final NodeInfoRetriever nodeInfoRetriever;


    @Override
    public String version() {
        return nodeInfoRetriever.getNodeClusterAndId();
    }
}
