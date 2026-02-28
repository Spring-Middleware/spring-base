package io.github.spring.middleware.registry.params;


import io.github.spring.middleware.registry.model.PublicServer;
import lombok.Data;

@Data
public class ResourceRegisterParameters {

    private String resourceName;
    private String cluster;
    private String node;
    private int port;
    private String path;
    private PublicServer publicServer;

}
