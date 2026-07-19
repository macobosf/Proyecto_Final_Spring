package ec.edu.ups.icc.academiceventsapi.event.repository;

import ec.edu.ups.icc.academiceventsapi.event.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findByEventIdOrderByStartAtAsc(Long eventId);

    boolean existsByEventIdAndTitleAndStartAt(Long eventId, String title, Instant startAt);
}
