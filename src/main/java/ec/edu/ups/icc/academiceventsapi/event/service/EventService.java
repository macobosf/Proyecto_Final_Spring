package ec.edu.ups.icc.academiceventsapi.event.service;

import ec.edu.ups.icc.academiceventsapi.event.dto.EventRequest;
import ec.edu.ups.icc.academiceventsapi.event.dto.EventResponse;
import ec.edu.ups.icc.academiceventsapi.event.entity.EventModality;
import ec.edu.ups.icc.academiceventsapi.event.entity.EventStatus;
import ec.edu.ups.icc.academiceventsapi.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface EventService {

    EventResponse create(EventRequest request, User organizer);

    EventResponse update(Long id, EventRequest request, User actor);

    EventResponse changeStatus(Long id, EventStatus newStatus, User actor);

    void delete(Long id, User actor);

    EventResponse getById(Long id, User actor);

    Page<EventResponse> listPublic(Long categoryId, EventModality modality, String search,
                                    Instant startFrom, Instant startTo, Pageable pageable);

    Page<EventResponse> listMine(User organizer, EventStatus status, Pageable pageable);
}
