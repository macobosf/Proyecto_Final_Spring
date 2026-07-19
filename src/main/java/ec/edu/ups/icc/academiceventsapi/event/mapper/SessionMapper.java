package ec.edu.ups.icc.academiceventsapi.event.mapper;

import ec.edu.ups.icc.academiceventsapi.event.dto.SessionResponse;
import ec.edu.ups.icc.academiceventsapi.event.entity.Session;
import org.springframework.stereotype.Component;

@Component
public class SessionMapper {

    public SessionResponse toResponse(Session session) {
        return new SessionResponse(
                session.getId(),
                session.getEvent().getId(),
                session.getTitle(),
                session.getDescription(),
                session.getStartAt(),
                session.getEndAt(),
                session.getLocation(),
                session.getVirtualUrl(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
