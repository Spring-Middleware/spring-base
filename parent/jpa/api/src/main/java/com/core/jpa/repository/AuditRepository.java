package com.core.jpa.repository;

import com.core.model.interfaces.Audit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository<A extends Audit, ID> extends JpaRepository<A,ID> {

}
