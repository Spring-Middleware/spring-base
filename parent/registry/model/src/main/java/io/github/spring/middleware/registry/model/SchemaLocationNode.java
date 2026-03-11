package io.github.spring.middleware.registry.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;

@Data
public class SchemaLocationNode {

    private UUID id;
    private String location;
    private String namespace;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "CET")
    private Timestamp lastAliveCheckDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "CET")
    private Timestamp startDate;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SchemaLocationNode that = (SchemaLocationNode) o;
        return Objects.equals(location, that.location) && Objects.equals(namespace, that.namespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, namespace);
    }
}
