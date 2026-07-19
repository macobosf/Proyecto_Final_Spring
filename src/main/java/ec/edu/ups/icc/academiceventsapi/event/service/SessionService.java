package ec.edu.ups.icc.academiceventsapi.event.service;

import ec.edu.ups.icc.academiceventsapi.event.dto.SessionRequest;
import ec.edu.ups.icc.academiceventsapi.event.dto.SessionResponse;
import ec.edu.ups.icc.academiceventsapi.user.entity.User;

import java.util.List;

public interface SessionService {

    SessionResponse create(Long eventId, SessionRequest request, User actor);

    SessionResponse update(Long eventId, Long sessionId, SessionRequest request, User actor);

    void delete(Long eventId, Long sessionId, User actor);

    List<SessionResponse> listByEvent(Long eventId, User actor);
}
