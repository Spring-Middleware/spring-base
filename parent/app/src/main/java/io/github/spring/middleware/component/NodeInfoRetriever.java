package io.github.spring.middleware.component;

import io.github.spring.middleware.register.resource.ResourceRegisterConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NodeInfoRetriever {

    private UUID nodeId;
    @Value("${server.servlet.context-path:}")
    private String contextPath;

    private final ResourceRegisterConfiguration resourceRegisterConfiguration;

    @PostConstruct
    public void init() {
        this.nodeId = UUID.randomUUID();
    }

    public UUID getNodeId() {
        return this.nodeId;
    }

    public String getAddress() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }

    public String getNodeClusterAndId() {
        return STR."\{resourceRegisterConfiguration.getClusterName()}-\{getNodeId()}";
    }

    public String getClusterName() {
        return resourceRegisterConfiguration.getClusterName();
    }

    public String getContextPath() {
        return contextPath;
    }

    public List<String> getMandatoryPublicPaths() {
        return List.of(
                "/_alive",
                "/graphql/_alive",
                "/resources/register",
                "/graphql/schema-metadata"
        );
    }

}
