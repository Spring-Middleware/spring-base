package io.github.spring.middleware.registry.params;

import lombok.Data;

@Data
public class SchemaRegisterParameters {

    private String namespace;
    private String location;
    private String pathApi;
    private String nodeLocation;
}
