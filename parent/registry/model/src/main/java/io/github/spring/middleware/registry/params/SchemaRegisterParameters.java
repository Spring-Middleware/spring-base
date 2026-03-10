package io.github.spring.middleware.registry.params;

import lombok.Data;

import java.util.UUID;

@Data
public class SchemaRegisterParameters {

    private String namespace;
    private String location;
    private String contextPath;
    private String pathApi;
    private UUID nodeId;
    private String nodeLocation;
}
