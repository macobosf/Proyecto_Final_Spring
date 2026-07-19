package ec.edu.ups.icc.academiceventsapi.event.service;

import ec.edu.ups.icc.academiceventsapi.category.entity.Category;
import ec.edu.ups.icc.academiceventsapi.category.repository.CategoryRepository;
import ec.edu.ups.icc.academiceventsapi.common.exception.BusinessRuleViolationException;
import ec.edu.ups.icc.academiceventsapi.common.exception.ResourceNotFoundException;
import ec.edu.ups.icc.academiceventsapi.event.dto.EventRequest;
import ec.edu.ups.icc.academiceventsapi.event.dto.EventResponse;
import ec.edu.ups.icc.academiceventsapi.event.entity.Event;
import ec.edu.ups.icc.academiceventsapi.event.entity.EventModality;
import ec.edu.ups.icc.academiceventsapi.event.entity.EventStatus;
import ec.edu.ups.icc.academiceventsapi.event.mapper.EventMapper;
import ec.edu.ups.icc.academiceventsapi.event.repository.EventRepository;
import ec.edu.ups.icc.academiceventsapi.event.repository.EventSpecifications;
import ec.edu.ups.icc.academiceventsapi.registration.entity.RegistrationStatus;
import ec.edu.ups.icc.academiceventsapi.registration.repository.RegistrationRepository;
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
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final RegistrationRepository registrationRepository;
    private final EventMapper eventMapper;

    public EventServiceImpl(EventRepository eventRepository, CategoryRepository categoryRepository,
                             RegistrationRepository registrationRepository, EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.registrationRepository = registrationRepository;
        this.eventMapper = eventMapper;
    }

    @Override
    @Transactional
    public EventResponse create(EventRequest request, User organizer) {
        validateEventDates(request);
        validateModalityData(request);
        Category category = resolveActiveCategory(request.categoryId());

        Event event = new Event(request.title(), request.description(), request.modality(), request.capacity(),
                request.registrationStartAt(), request.registrationEndAt(), request.startAt(), request.endAt(),
                organizer, category);
        event.setLocation(blankToNull(request.location()));
        event.setVirtualUrl(blankToNull(request.virtualUrl()));

        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public EventResponse update(Long id, EventRequest request, User actor) {
        Event event = findByIdOrThrow(id);
        assertOwnerOrAdmin(event, actor);
        validateEventDates(request);
        validateModalityData(request);

        if (!event.getCategory().getId().equals(request.categoryId())) {
            event.setCategory(resolveActiveCategory(request.categoryId()));
        }

        if (!event.getCapacity().equals(request.capacity())) {
            long confirmed = registrationRepository.countByEventIdAndStatus(id, RegistrationStatus.CONFIRMED);
            if (request.capacity() < confirmed) {
                throw new BusinessRuleViolationException("CAPACITY_BELOW_CONFIRMED",
                        "El nuevo cupo no puede ser menor a las inscripciones ya confirmadas (" + confirmed + ").");
            }
            event.setAvailableCapacity((int) (request.capacity() - confirmed));
            event.setCapacity(request.capacity());
        }

        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setModality(request.modality());
        event.setLocation(blankToNull(request.location()));
        event.setVirtualUrl(blankToNull(request.virtualUrl()));
        event.setRegistrationStartAt(request.registrationStartAt());
        event.setRegistrationEndAt(request.registrationEndAt());
        event.setStartAt(request.startAt());
        event.setEndAt(request.endAt());

        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional
    public EventResponse changeStatus(Long id, EventStatus newStatus, User actor) {
        Event event = findByIdOrThrow(id);
        assertOwnerOrAdmin(event, actor);

        if (!isValidTransition(event.getStatus(), newStatus)) {
            throw new BusinessRuleViolationException("INVALID_STATUS_TRANSITION",
                    "No se puede cambiar el evento de " + event.getStatus() + " a " + newStatus + ".");
        }

        event.setStatus(newStatus);
        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional
    public void delete(Long id, User actor) {
        Event event = findByIdOrThrow(id);
        assertOwnerOrAdmin(event, actor);

        if (event.getStatus() == EventStatus.PUBLISHED && registrationRepository.existsByEventId(id)) {
            throw new BusinessRuleViolationException("EVENT_HAS_REGISTRATIONS",
                    "No se puede eliminar un evento publicado que ya tiene inscripciones. Cancélelo primero.");
        }

        event.setDeleted(true);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getById(Long id, User actor) {
        Event event = findByIdOrThrow(id);

        boolean isOwnerOrAdmin = actor != null
                && (actor.hasRole(RoleName.ADMIN) || event.getOrganizer().getId().equals(actor.getId()));

        if (event.getStatus() != EventStatus.PUBLISHED && !isOwnerOrAdmin) {
            throw new ResourceNotFoundException("No se encontró el evento solicitado.");
        }

        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> listPublic(Long categoryId, EventModality modality, String search,
                                           Instant startFrom, Instant startTo, Pageable pageable) {
        Specification<Event> spec = Specification.allOf(
                EventSpecifications.notDeleted(),
                EventSpecifications.hasStatus(EventStatus.PUBLISHED),
                EventSpecifications.hasCategory(categoryId),
                EventSpecifications.hasModality(modality),
                EventSpecifications.titleContains(search),
                EventSpecifications.startAtFrom(startFrom),
                EventSpecifications.startAtTo(startTo)
        );
        return eventRepository.findAll(spec, pageable).map(eventMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> listMine(User organizer, EventStatus status, Pageable pageable) {
        Specification<Event> spec = Specification.allOf(
                EventSpecifications.notDeleted(),
                EventSpecifications.hasOrganizer(organizer.getId()),
                EventSpecifications.hasStatus(status)
        );
        return eventRepository.findAll(spec, pageable).map(eventMapper::toResponse);
    }

    private Event findByIdOrThrow(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el evento solicitado."));
        if (event.isDeleted()) {
            throw new ResourceNotFoundException("No se encontró el evento solicitado.");
        }
        return event;
    }

    private void assertOwnerOrAdmin(Event event, User actor) {
        if (!actor.hasRole(RoleName.ADMIN) && !event.getOrganizer().getId().equals(actor.getId())) {
            throw new AccessDeniedException("No puede modificar eventos de otro organizador.");
        }
    }

    private Category resolveActiveCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la categoría indicada."));
        if (!category.isActive()) {
            throw new BusinessRuleViolationException("CATEGORY_INACTIVE",
                    "La categoría indicada no está activa y no puede utilizarse en nuevos eventos.");
        }
        return category;
    }

    private void validateEventDates(EventRequest request) {
        boolean valid = request.registrationStartAt().isBefore(request.registrationEndAt())
                && !request.registrationEndAt().isAfter(request.startAt())
                && request.startAt().isBefore(request.endAt());

        if (!valid) {
            throw new BusinessRuleViolationException("INVALID_EVENT_DATES",
                    "Las fechas deben cumplir: inicio de inscripciones < fin de inscripciones <= "
                            + "inicio del evento < fin del evento.");
        }
    }

    private void validateModalityData(EventRequest request) {
        String location = blankToNull(request.location());
        String virtualUrl = blankToNull(request.virtualUrl());

        boolean valid = switch (request.modality()) {
            case PRESENTIAL -> location != null && virtualUrl == null;
            case VIRTUAL -> virtualUrl != null && location == null;
            case HYBRID -> location != null && virtualUrl != null;
        };

        if (!valid) {
            throw new BusinessRuleViolationException("INVALID_MODALITY_DATA",
                    "Los datos de ubicación/enlace no son consistentes con la modalidad del evento.");
        }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private boolean isValidTransition(EventStatus current, EventStatus target) {
        return switch (current) {
            case DRAFT -> target == EventStatus.PUBLISHED || target == EventStatus.CANCELLED;
            case PUBLISHED -> target == EventStatus.FINISHED || target == EventStatus.CANCELLED;
            case FINISHED, CANCELLED -> false;
        };
    }
}
