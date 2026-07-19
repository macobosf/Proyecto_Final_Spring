package ec.edu.ups.icc.academiceventsapi.event.mapper;

import ec.edu.ups.icc.academiceventsapi.event.dto.EventResponse;
import ec.edu.ups.icc.academiceventsapi.event.entity.Event;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getModality(),
                event.getLocation(),
                event.getVirtualUrl(),
                event.getCapacity(),
                event.getAvailableCapacity(),
                event.getRegistrationStartAt(),
                event.getRegistrationEndAt(),
                event.getStartAt(),
                event.getEndAt(),
                event.getStatus(),
                event.getOrganizer().getId(),
                event.getOrganizer().getFirstName() + " " + event.getOrganizer().getLastName(),
                event.getCategory().getId(),
                event.getCategory().getName(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
