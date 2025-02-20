package com.core.model.entities;

import com.core.model.interfaces.Audit;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;

import java.time.LocalDateTime;

@MappedSuperclass
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class AuditInfo implements Audit {

    @Column(name = "VERSION")
    @JsonIgnore
    private Integer version;

    @Column(name = "AU_USERIDINS")
    private Integer idUserInsert;

    @Column(name = "INSERTDATE")
    @JsonIgnore
    private LocalDateTime insertDate;

    @Column(name = "AU_USERIDUPD")
    private Integer idUserUpdate;

    @Column(name = "MODIFYDATE")
    @JsonIgnore
    private LocalDateTime modifyDate;

    @Column(name = "DBUSERINS")
    @JsonIgnore
    private String dbUserIns;

    @Column(name = "DBUSERUPD")
    @JsonIgnore
    private String dbUserUpdate;

    @PrePersist
    private void prePersist() {
        insertDate = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        modifyDate = LocalDateTime.now();
    }

}
