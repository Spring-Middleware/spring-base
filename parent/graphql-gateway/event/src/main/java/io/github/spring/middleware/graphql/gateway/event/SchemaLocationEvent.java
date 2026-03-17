package io.github.spring.middleware.graphql.gateway.event;

import lombok.Data;

@Data
public class SchemaLocationEvent {

    private SchemaLocationEventType eventType;
    private String namespace;

}
