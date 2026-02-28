package io.github.spring.middleware.registry.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.sql.Timestamp;
import java.util.Set;

@Data
public class SchemaLocation {

    private Integer id;
    private String namespace;
    private String schemaVersion;
    private String location;
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
    private Set<SchemaLocationNode> schemaLocationNodes;

}
