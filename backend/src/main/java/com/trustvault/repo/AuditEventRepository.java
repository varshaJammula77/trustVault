package com.trustvault.repo;

import com.trustvault.model.AuditEvent;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface AuditEventRepository extends JpaRepository<AuditEvent,String> {
    Optional<AuditEvent> findTopByOrderBySequenceNumberDesc();
    List<AuditEvent> findTop500ByOrderBySequenceNumberDesc();
    List<AuditEvent> findAllByOrderBySequenceNumberAsc();
    long countByAction(com.trustvault.model.AuditAction action);
}
