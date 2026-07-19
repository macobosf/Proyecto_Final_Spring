package ec.edu.ups.icc.academiceventsapi.audit.repository;

import ec.edu.ups.icc.academiceventsapi.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
