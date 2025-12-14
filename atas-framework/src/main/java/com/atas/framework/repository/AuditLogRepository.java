package com.atas.framework.repository;

import com.atas.framework.model.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for AuditLog entities. */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
  List<AuditLog> findByUsername(String username);

  List<AuditLog> findByAction(String action);

  Page<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

  Page<AuditLog> findByUsernameAndTimestampBetween(
      String username, LocalDateTime start, LocalDateTime end, Pageable pageable);
}
