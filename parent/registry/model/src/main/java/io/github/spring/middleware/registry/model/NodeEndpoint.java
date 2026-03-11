package io.github.spring.middleware.registry.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NodeEndpoint {
    private UUID id;
    private String nodeEndpoint;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NodeEndpoint that = (NodeEndpoint) o;
        return Objects.equals(nodeEndpoint, that.nodeEndpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nodeEndpoint);
    }
}
