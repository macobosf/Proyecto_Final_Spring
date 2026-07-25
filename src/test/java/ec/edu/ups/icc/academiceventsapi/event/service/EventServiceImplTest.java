package ec.edu.ups.icc.academiceventsapi.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

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
import ec.edu.ups.icc.academiceventsapi.registration.repository.RegistrationRepository;
import ec.edu.ups.icc.academiceventsapi.user.entity.RoleName;
import ec.edu.ups.icc.academiceventsapi.user.entity.User;

@ExtendWith(MockitoExtension.class)
public class EventServiceImplTest {

    @InjectMocks
    private EventServiceImpl eventServiceImpl;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private EventMapper eventMapper;

    private EventRequest validPresentialRequest(Long categoryId) {
        Instant now = Instant.now();
        return new EventRequest("Conferencia Java", "Descripción del evento", EventModality.PRESENTIAL,
                "Auditorio Central", null, 100, now, now.plusSeconds(3600),
                now.plusSeconds(7200), now.plusSeconds(10800), categoryId);
    }

    private EventResponse sampleEventResponse() {
        Instant now = Instant.now();
        return new EventResponse(1L, "Conferencia Java", "Descripción del evento", EventModality.PRESENTIAL,
                "Auditorio Central", null, 100, 100, now, now.plusSeconds(3600),
                now.plusSeconds(7200), now.plusSeconds(10800), EventStatus.DRAFT, 1L, "Juan Pérez",
                1L, "Tecnología", now, now);
    }

    private Event newEvent(User organizer, Category category) {
        Instant now = Instant.now();
        return new Event("Conferencia Java", "Descripción del evento", EventModality.PRESENTIAL, 100,
                now, now.plusSeconds(3600), now.plusSeconds(7200), now.plusSeconds(10800), organizer, category);
    }

    // Datos válidos: debe guardar el evento y devolverlo mapeado.
    @Test
    void create_deberiaCrearEvento_cuandoDatosValidos() {
        User organizer = mock(User.class);
        Category category = new Category("Tecnología", "Eventos de tecnología");
        EventRequest request = validPresentialRequest(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(sampleEventResponse());

        EventResponse response = eventServiceImpl.create(request, organizer);

        assertThat(response).isNotNull();
        verify(eventRepository).save(any(Event.class));
    }

    // Fecha de fin de inscripciones anterior a la de inicio: debe fallar sin llegar a guardar.
    @Test
    void create_deberiaLanzarExcepcion_cuandoFechasInvalidas() {
        User organizer = mock(User.class);
        Instant now = Instant.now();
        EventRequest request = new EventRequest("Conferencia Java", "Descripción", EventModality.PRESENTIAL,
                "Auditorio Central", null, 100, now, now.minusSeconds(3600),
                now.plusSeconds(7200), now.plusSeconds(10800), 1L);

        assertThrows(BusinessRuleViolationException.class, () -> eventServiceImpl.create(request, organizer));
        verify(eventRepository, never()).save(any());
    }

    // Modalidad VIRTUAL con ubicación física y sin enlace: datos inconsistentes con la modalidad.
    @Test
    void create_deberiaLanzarExcepcion_cuandoModalidadInconsistente() {
        User organizer = mock(User.class);
        Instant now = Instant.now();
        EventRequest request = new EventRequest("Conferencia Java", "Descripción", EventModality.VIRTUAL,
                "Auditorio Central", null, 100, now, now.plusSeconds(3600),
                now.plusSeconds(7200), now.plusSeconds(10800), 1L);

        assertThrows(BusinessRuleViolationException.class, () -> eventServiceImpl.create(request, organizer));
        verify(eventRepository, never()).save(any());
    }

    // Categoría inactiva: no puede usarse en eventos nuevos.
    @Test
    void create_deberiaLanzarExcepcion_cuandoCategoriaInactiva() {
        User organizer = mock(User.class);
        Category category = new Category("Tecnología", "Eventos de tecnología");
        category.setActive(false);
        EventRequest request = validPresentialRequest(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThrows(BusinessRuleViolationException.class, () -> eventServiceImpl.create(request, organizer));
        verify(eventRepository, never()).save(any());
    }

    // El propietario del evento puede actualizarlo.
    @Test
    void update_deberiaActualizarEvento_cuandoEsPropietario() {
        User organizer = mock(User.class);
        when(organizer.getId()).thenReturn(1L);
        when(organizer.hasRole(RoleName.ADMIN)).thenReturn(false);

        Category category = new Category("Tecnología", "Eventos de tecnología");
        ReflectionTestUtils.setField(category, "id", 1L);
        Event event = newEvent(organizer, category);

        EventRequest request = validPresentialRequest(1L);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(sampleEventResponse());

        EventResponse response = eventServiceImpl.update(1L, request, organizer);

        assertThat(response).isNotNull();
        assertThat(event.getTitle()).isEqualTo(request.title());
    }

    // Un organizador distinto al dueño (y sin ser ADMIN) no puede modificar el evento.
    @Test
    void update_deberiaLanzarExcepcion_cuandoNoEsPropietarioNiAdmin() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(1L);

        User otherOrganizer = mock(User.class);
        when(otherOrganizer.hasRole(RoleName.ADMIN)).thenReturn(false);
        when(otherOrganizer.getId()).thenReturn(2L);

        Category category = new Category("Tecnología", "Eventos de tecnología");
        Event event = newEvent(owner, category);

        EventRequest request = validPresentialRequest(1L);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(AccessDeniedException.class, () -> eventServiceImpl.update(1L, request, otherOrganizer));
    }

    // Transición DRAFT -> PUBLISHED es válida.
    @Test
    void changeStatus_deberiaActualizarEstado_cuandoTransicionValida() {
        User organizer = mock(User.class);
        when(organizer.getId()).thenReturn(1L);
        when(organizer.hasRole(RoleName.ADMIN)).thenReturn(false);

        Category category = new Category("Tecnología", "Eventos de tecnología");
        Event event = newEvent(organizer, category);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(sampleEventResponse());

        EventResponse response = eventServiceImpl.changeStatus(1L, EventStatus.PUBLISHED, organizer);

        assertThat(response).isNotNull();
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    // Transición DRAFT -> FINISHED no está permitida.
    @Test
    void changeStatus_deberiaLanzarExcepcion_cuandoTransicionInvalida() {
        User organizer = mock(User.class);
        when(organizer.getId()).thenReturn(1L);
        when(organizer.hasRole(RoleName.ADMIN)).thenReturn(false);

        Category category = new Category("Tecnología", "Eventos de tecnología");
        Event event = newEvent(organizer, category);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(BusinessRuleViolationException.class,
                () -> eventServiceImpl.changeStatus(1L, EventStatus.FINISHED, organizer));
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    // Evento publicado sin inscripciones: se puede eliminar (soft delete).
    @Test
    void delete_deberiaEliminarEvento_cuandoNoTieneInscripciones() {
        User organizer = mock(User.class);
        when(organizer.getId()).thenReturn(1L);
        when(organizer.hasRole(RoleName.ADMIN)).thenReturn(false);

        Category category = new Category("Tecnología", "Eventos de tecnología");
        Event event = newEvent(organizer, category);
        event.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByEventId(1L)).thenReturn(false);

        eventServiceImpl.delete(1L, organizer);

        assertThat(event.isDeleted()).isTrue();
    }

    // Evento publicado con inscripciones: no se puede eliminar directamente.
    @Test
    void delete_deberiaLanzarExcepcion_cuandoPublicadoConInscripciones() {
        User organizer = mock(User.class);
        when(organizer.getId()).thenReturn(1L);
        when(organizer.hasRole(RoleName.ADMIN)).thenReturn(false);

        Category category = new Category("Tecnología", "Eventos de tecnología");
        Event event = newEvent(organizer, category);
        event.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByEventId(1L)).thenReturn(true);

        assertThrows(BusinessRuleViolationException.class, () -> eventServiceImpl.delete(1L, organizer));
        assertThat(event.isDeleted()).isFalse();
    }

    // Evento publicado: visible para cualquiera, incluso sin usuario autenticado.
    @Test
    void getById_deberiaRetornarEvento_cuandoEsPublico() {
        User organizer = mock(User.class);
        Category category = new Category("Tecnología", "Eventos de tecnología");
        Event event = newEvent(organizer, category);
        event.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(sampleEventResponse());

        EventResponse response = eventServiceImpl.getById(1L, null);

        assertThat(response).isNotNull();
    }

    // Evento en borrador consultado por alguien que no es el dueño: se oculta como si no existiera.
    @Test
    void getById_deberiaLanzarExcepcion_cuandoNoPublicadoYNoEsPropietario() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(1L);

        User other = mock(User.class);
        when(other.hasRole(RoleName.ADMIN)).thenReturn(false);
        when(other.getId()).thenReturn(2L);

        Category category = new Category("Tecnología", "Eventos de tecnología");
        Event event = newEvent(owner, category);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(ResourceNotFoundException.class, () -> eventServiceImpl.getById(1L, other));
    }
}
