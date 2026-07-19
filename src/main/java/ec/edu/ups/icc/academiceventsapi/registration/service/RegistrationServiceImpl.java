package ec.edu.ups.icc.academiceventsapi.registration.service;

import ec.edu.ups.icc.academiceventsapi.common.exception.BusinessRuleViolationException;
import ec.edu.ups.icc.academiceventsapi.common.exception.DuplicateResourceException;
import ec.edu.ups.icc.academiceventsapi.common.exception.ResourceNotFoundException;
import ec.edu.ups.icc.academiceventsapi.event.entity.Event;
import ec.edu.ups.icc.academiceventsapi.event.entity.EventStatus;
import ec.edu.ups.icc.academiceventsapi.event.repository.EventRepository;
import ec.edu.ups.icc.academiceventsapi.registration.dto.RegistrationResponse;
import ec.edu.ups.icc.academiceventsapi.registration.entity.Registration;
import ec.edu.ups.icc.academiceventsapi.registration.entity.RegistrationStatus;
import ec.edu.ups.icc.academiceventsapi.registration.mapper.RegistrationMapper;
import ec.edu.ups.icc.academiceventsapi.registration.repository.RegistrationRepository;
import ec.edu.ups.icc.academiceventsapi.registration.repository.RegistrationSpecifications;
import ec.edu.ups.icc.academiceventsapi.user.entity.RoleName;
import ec.edu.ups.icc.academiceventsapi.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final RegistrationMapper registrationMapper;

    public RegistrationServiceImpl(RegistrationRepository registrationRepository, EventRepository eventRepository,
                                    RegistrationMapper registrationMapper) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.registrationMapper = registrationMapper;
    }

    @Override
    @Transactional
    public RegistrationResponse register(Long eventId, User participant) {
        Event event = findEventOrThrow(eventId);

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleViolationException("EVENT_NOT_PUBLISHED",
                    "Solo se puede inscribir en eventos publicados.");
        }

        Instant now = Instant.now();
        if (now.isBefore(event.getRegistrationStartAt()) || now.isAfter(event.getRegistrationEndAt())) {
            throw new BusinessRuleViolationException("REGISTRATION_CLOSED",
                    "El período de inscripciones para este evento no está abierto.");
        }

        if (event.getAvailableCapacity() <= 0) {
            throw new BusinessRuleViolationException("EVENT_FULL", "El evento no tiene cupos disponibles.");
        }

        if (registrationRepository.existsByEventIdAndParticipantId(eventId, participant.getId())) {
            throw new DuplicateResourceException("Ya tiene una inscripción registrada para este evento.");
        }

        Registration registration = new Registration(event, participant);
        return registrationMapper.toResponse(registrationRepository.save(registration));
    }

    @Override
    @Transactional
    public RegistrationResponse cancel(Long id, User participant) {
        Registration registration = findRegistrationOrThrow(id);

        if (!registration.getParticipant().getId().equals(participant.getId())) {
            throw new AccessDeniedException("No puede cancelar inscripciones de otro participante.");
        }

        if (registration.getStatus() == RegistrationStatus.CANCELLED
                || registration.getStatus() == RegistrationStatus.REJECTED) {
            throw new BusinessRuleViolationException("REGISTRATION_NOT_CANCELLABLE",
                    "La inscripción ya no se puede cancelar desde su estado actual.");
        }

        boolean wasConfirmed = registration.getStatus() == RegistrationStatus.CONFIRMED;

        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(Instant.now());
        registration.setStatusUpdatedAt(Instant.now());

        if (wasConfirmed) {
            Event event = registration.getEvent();
            event.setAvailableCapacity(event.getAvailableCapacity() + 1);
        }

        return registrationMapper.toResponse(registration);
    }

    @Override
    @Transactional
    public RegistrationResponse changeStatus(Long eventId, Long id, RegistrationStatus newStatus, User actor) {
        if (newStatus != RegistrationStatus.CONFIRMED && newStatus != RegistrationStatus.REJECTED) {
            throw new BusinessRuleViolationException("INVALID_STATUS_TRANSITION",
                    "Solo se puede confirmar o rechazar una inscripción desde este endpoint.");
        }

        Event event = findEventOrThrow(eventId);
        assertEventOwnerOrAdmin(event, actor);

        Registration registration = findRegistrationOrThrow(id);
        if (!registration.getEvent().getId().equals(eventId)) {
            throw new ResourceNotFoundException("No se encontró la inscripción solicitada.");
        }
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new BusinessRuleViolationException("INVALID_STATUS_TRANSITION",
                    "Solo se pueden confirmar o rechazar inscripciones en estado PENDING.");
        }

        if (newStatus == RegistrationStatus.CONFIRMED) {
            if (event.getAvailableCapacity() <= 0) {
                throw new BusinessRuleViolationException("EVENT_FULL", "El evento ya no tiene cupos disponibles.");
            }
            event.setAvailableCapacity(event.getAvailableCapacity() - 1);
            registration.setConfirmedAt(Instant.now());
        }

        registration.setStatus(newStatus);
        registration.setStatusUpdatedAt(Instant.now());

        return registrationMapper.toResponse(registration);
    }

    @Override
    @Transactional(readOnly = true)
    public RegistrationResponse getById(Long id, User actor) {
        Registration registration = findRegistrationOrThrow(id);

        boolean isParticipant = registration.getParticipant().getId().equals(actor.getId());
        boolean isEventOwner = registration.getEvent().getOrganizer().getId().equals(actor.getId());

        if (!isParticipant && !isEventOwner && !actor.hasRole(RoleName.ADMIN)) {
            throw new AccessDeniedException("No tiene permisos para ver esta inscripción.");
        }

        return registrationMapper.toResponse(registration);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegistrationResponse> listMine(User participant, RegistrationStatus status, Pageable pageable) {
        Specification<Registration> spec = Specification.allOf(
                RegistrationSpecifications.byParticipant(participant.getId()),
                RegistrationSpecifications.hasStatus(status)
        );
        return registrationRepository.findAll(spec, pageable).map(registrationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegistrationResponse> listByEvent(Long eventId, RegistrationStatus status, User actor,
                                                   Pageable pageable) {
        Event event = findEventOrThrow(eventId);
        assertEventOwnerOrAdmin(event, actor);

        Specification<Registration> spec = Specification.allOf(
                RegistrationSpecifications.byEvent(eventId),
                RegistrationSpecifications.hasStatus(status)
        );
        return registrationRepository.findAll(spec, pageable).map(registrationMapper::toResponse);
    }

    private Event findEventOrThrow(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el evento solicitado."));
        if (event.isDeleted()) {
            throw new ResourceNotFoundException("No se encontró el evento solicitado.");
        }
        return event;
    }

    private Registration findRegistrationOrThrow(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la inscripción solicitada."));
    }

    private void assertEventOwnerOrAdmin(Event event, User actor) {
        if (!actor.hasRole(RoleName.ADMIN) && !event.getOrganizer().getId().equals(actor.getId())) {
            throw new AccessDeniedException("No puede gestionar inscripciones de eventos de otro organizador.");
        }
    }
}
