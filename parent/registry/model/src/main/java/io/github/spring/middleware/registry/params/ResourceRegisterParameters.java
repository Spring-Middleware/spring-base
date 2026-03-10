package io.github.spring.middleware.registry.params;


import io.github.spring.middleware.registry.model.PublicServer;
import lombok.Data;

import java.util.UUID;

@Data
public class ResourceRegisterParameters {

    private UUID nodeId;
    private String resourceName;
    private String cluster;
    private String node;
    private int port;
    private String path;
    private String contextPath;
    private PublicServer publicServer;

}
