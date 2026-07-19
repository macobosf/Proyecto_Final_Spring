package ec.edu.ups.icc.academiceventsapi.event.repository;

import ec.edu.ups.icc.academiceventsapi.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
}
