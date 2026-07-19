package ec.edu.ups.icc.academiceventsapi.event.service;

import ec.edu.ups.icc.academiceventsapi.common.exception.BusinessRuleViolationException;
import ec.edu.ups.icc.academiceventsapi.common.exception.DuplicateResourceException;
import ec.edu.ups.icc.academiceventsapi.common.exception.ResourceNotFoundException;
import ec.edu.ups.icc.academiceventsapi.event.dto.SessionRequest;
import ec.edu.ups.icc.academiceventsapi.event.dto.SessionResponse;
import ec.edu.ups.icc.academiceventsapi.event.entity.Event;
import ec.edu.ups.icc.academiceventsapi.event.entity.EventStatus;
import ec.edu.ups.icc.academiceventsapi.event.entity.Session;
import ec.edu.ups.icc.academiceventsapi.event.mapper.SessionMapper;
import ec.edu.ups.icc.academiceventsapi.event.repository.EventRepository;
import ec.edu.ups.icc.academiceventsapi.event.repository.SessionRepository;
import ec.edu.ups.icc.academiceventsapi.user.entity.RoleName;
import ec.edu.ups.icc.academiceventsapi.user.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SessionServiceImpl implements SessionService {

    private final EventRepository eventRepository;
    private final SessionRepository sessionRepository;
    private final SessionMapper sessionMapper;

    public SessionServiceImpl(EventRepository eventRepository, SessionRepository sessionRepository,
                               SessionMapper sessionMapper) {
        this.eventRepository = eventRepository;
        this.sessionRepository = sessionRepository;
        this.sessionMapper = sessionMapper;
    }

    @Override
    @Transactional
    public SessionResponse create(Long eventId, SessionRequest request, User actor) {
        Event event = findEventOrThrow(eventId);
        assertOwnerOrAdmin(event, actor);
        validateSessionDates(request);

        if (sessionRepository.existsByEventIdAndTitleAndStartAt(eventId, request.title(), request.startAt())) {
            throw new DuplicateResourceException(
                    "Ya existe una sesión con ese título y hora de inicio para este evento.");
        }

        Session session = new Session(event, request.title(), request.startAt(), request.endAt());
        session.setDescription(request.description());
        session.setLocation(request.location());
        session.setVirtualUrl(request.virtualUrl());

        return sessionMapper.toResponse(sessionRepository.save(session));
    }

    @Override
    @Transactional
    public SessionResponse update(Long eventId, Long sessionId, SessionRequest request, User actor) {
        Event event = findEventOrThrow(eventId);
        assertOwnerOrAdmin(event, actor);
        Session session = findSessionOrThrow(eventId, sessionId);
        validateSessionDates(request);

        session.setTitle(request.title());
        session.setDescription(request.description());
        session.setStartAt(request.startAt());
        session.setEndAt(request.endAt());
        session.setLocation(request.location());
        session.setVirtualUrl(request.virtualUrl());

        return sessionMapper.toResponse(session);
    }

    @Override
    @Transactional
    public void delete(Long eventId, Long sessionId, User actor) {
        Event event = findEventOrThrow(eventId);
        assertOwnerOrAdmin(event, actor);
        Session session = findSessionOrThrow(eventId, sessionId);
        sessionRepository.delete(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> listByEvent(Long eventId, User actor) {
        Event event = findEventOrThrow(eventId);

        boolean isOwnerOrAdmin = actor != null
                && (actor.hasRole(RoleName.ADMIN) || event.getOrganizer().getId().equals(actor.getId()));

        if (event.getStatus() != EventStatus.PUBLISHED && !isOwnerOrAdmin) {
            throw new ResourceNotFoundException("No se encontró el evento solicitado.");
        }

        return sessionRepository.findByEventIdOrderByStartAtAsc(eventId).stream()
                .map(sessionMapper::toResponse)
                .toList();
    }

    private Event findEventOrThrow(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el evento solicitado."));
        if (event.isDeleted()) {
            throw new ResourceNotFoundException("No se encontró el evento solicitado.");
        }
        return event;
    }

    private Session findSessionOrThrow(Long eventId, Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la sesión solicitada."));
        if (!session.getEvent().getId().equals(eventId)) {
            throw new ResourceNotFoundException("No se encontró la sesión solicitada.");
        }
        return session;
    }

    private void assertOwnerOrAdmin(Event event, User actor) {
        if (!actor.hasRole(RoleName.ADMIN) && !event.getOrganizer().getId().equals(actor.getId())) {
            throw new AccessDeniedException("No puede modificar sesiones de eventos de otro organizador.");
        }
    }

    private void validateSessionDates(SessionRequest request) {
        if (!request.startAt().isBefore(request.endAt())) {
            throw new BusinessRuleViolationException("INVALID_SESSION_DATES",
                    "La fecha de inicio de la sesión debe ser anterior a la fecha de fin.");
        }
    }
}
