package io.github.spring.middleware.registry.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Data
public class SchemaLocation {

    private Integer id;
    private String namespace;
    private String schemaVersion;
    private String location;
    private String contextPath;
    private String pathApi;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "CET")
    private Timestamp createDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "CET")
    private Timestamp updateDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "CET")
    private Timestamp lastDownDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "CET")
    private Timestamp lastErrorDate;
    private Integer totalTimeoutsLast24h;
    private Integer totalErrorsLast24h;
    private Integer totalOks;
    private Long responseTime;
    private Set<SchemaLocationNode> schemaLocationNodes = new HashSet<>();

    public void upsertSchemaLocationNode(SchemaLocationNode schemaLocationNode) {
        this.schemaLocationNodes.remove(schemaLocationNode);
        this.schemaLocationNodes.add(schemaLocationNode);
    }

    public Set<SchemaLocationNode> getSchemaLocationNodes() {
        return Set.copyOf(schemaLocationNodes);
    }

    public void removeSchemaLocationNodeByLocation(String nodeLocation) {
        this.schemaLocationNodes.removeIf(n -> nodeLocation.equalsIgnoreCase(n.getLocation()));
    }

    public void refreshSchemaLocationNodeLastAliveCheckDate(String location) {
        this.schemaLocationNodes.stream()
                .filter(node -> node.getLocation().equals(location))
                .findFirst()
                .ifPresent(node -> node.setLastAliveCheckDate(new Timestamp(System.currentTimeMillis())));
    }
}
