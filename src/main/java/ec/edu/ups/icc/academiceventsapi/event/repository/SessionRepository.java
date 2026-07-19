package ec.edu.ups.icc.academiceventsapi.event.repository;

import ec.edu.ups.icc.academiceventsapi.event.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, Long> {
}
