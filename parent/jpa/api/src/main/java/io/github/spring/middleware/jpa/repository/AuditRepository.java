package io.github.spring.middleware.jpa.repository;

import io.github.spring.middleware.model.interfaces.Audit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository<A extends Audit, ID> extends JpaRepository<A,ID> {

}
