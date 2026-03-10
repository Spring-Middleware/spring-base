package io.github.spring.middleware.registry.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NodeEndpoint {
    private UUID id;
    private String nodeEndpoint;
}
