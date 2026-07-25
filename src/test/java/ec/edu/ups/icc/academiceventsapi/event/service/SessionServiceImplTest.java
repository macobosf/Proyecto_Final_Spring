package ec.edu.ups.icc.academiceventsapi.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import ec.edu.ups.icc.academiceventsapi.category.entity.Category;
import ec.edu.ups.icc.academiceventsapi.common.exception.BusinessRuleViolationException;
import ec.edu.ups.icc.academiceventsapi.common.exception.DuplicateResourceException;
import ec.edu.ups.icc.academiceventsapi.common.exception.ResourceNotFoundException;
import ec.edu.ups.icc.academiceventsapi.event.dto.SessionRequest;
import ec.edu.ups.icc.academiceventsapi.event.dto.SessionResponse;
import ec.edu.ups.icc.academiceventsapi.event.entity.Event;
import ec.edu.ups.icc.academiceventsapi.event.entity.EventModality;
import ec.edu.ups.icc.academiceventsapi.event.entity.EventStatus;
import ec.edu.ups.icc.academiceventsapi.event.entity.Session;
import ec.edu.ups.icc.academiceventsapi.event.mapper.SessionMapper;
import ec.edu.ups.icc.academiceventsapi.event.repository.EventRepository;
import ec.edu.ups.icc.academiceventsapi.event.repository.SessionRepository;
import ec.edu.ups.icc.academiceventsapi.user.entity.RoleName;
import ec.edu.ups.icc.academiceventsapi.user.entity.User;

@ExtendWith(MockitoExtension.class)
public class SessionServiceImplTest {

    @InjectMocks
    private SessionServiceImpl sessionServiceImpl;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private SessionMapper sessionMapper;

    private Event eventWithId(Long id, User organizer) {
        Instant now = Instant.now();
        Event event = new Event("Conferencia Java", "Descripción", EventModality.PRESENTIAL, 100,
                now, now.plusSeconds(3600), now.plusSeconds(7200), now.plusSeconds(10800),
                organizer, new Category("Tecnología", "Eventos de tecnología"));
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }

    private SessionRequest validSessionRequest() {
        Instant now = Instant.now();
        return new SessionRequest("Charla de apertura", "Descripción", now.plusSeconds(3600),
                now.plusSeconds(7200), "Auditorio", null);
    }

    private SessionResponse sampleSessionResponse() {
        Instant now = Instant.now();
        return new SessionResponse(5L, 10L, "Charla de apertura", "Descripción", now,
                now.plusSeconds(3600), "Auditorio", null, now, now);
    }

    // Datos válidos, sin choque de horario: debe guardar la sesión.
    @Test
    void create_deberiaCrearSesion_cuandoDatosValidos() {
        User organizer = mock(User.class);
        when(organizer.getId()).thenReturn(1L);
        when(organizer.hasRole(RoleName.ADMIN)).thenReturn(false);
        Event event = eventWithId(10L, organizer);
        SessionRequest request = validSessionRequest();

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(sessionRepository.existsByEventIdAndTitleAndStartAt(10L, request.title(), request.startAt()))
                .thenReturn(false);
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionMapper.toResponse(any(Session.class))).thenReturn(sampleSessionResponse());

        SessionResponse response = sessionServiceImpl.create(10L, request, organizer);

        assertThat(response).isNotNull();
        verify(sessionRepository).save(any(Session.class));
    }

    // Quien no es dueño del evento (ni ADMIN) no puede crearle sesiones.
    @Test
    void create_deberiaLanzarExcepcion_cuandoNoEsPropietarioNiAdmin() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(1L);
        Event event = eventWithId(10L, owner);

        User other = mock(User.class);
        when(other.hasRole(RoleName.ADMIN)).thenReturn(false);
        when(other.getId()).thenReturn(2L);

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        SessionRequest request = validSessionRequest();
        assertThrows(AccessDeniedException.class, () -> sessionServiceImpl.create(10L, request, other));
        verify(sessionRepository, never()).save(any());
    }

    // Fecha de fin anterior a la de inicio: no es una sesión válida.
    @Test
    void create_deberiaLanzarExcepcion_cuandoFechasInvalidas() {
        User organizer = mock(User.class);
        when(organizer.getId()).thenReturn(1L);
        when(organizer.hasRole(RoleName.ADMIN)).thenReturn(false);
        Event event = eventWithId(10L, organizer);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        Instant now = Instant.now();
        SessionRequest request = new SessionRequest("Charla", "Desc", now.plusSeconds(7200),
                now.plusSeconds(3600), "Auditorio", null);

        assertThrows(BusinessRuleViolationException.class, () -> sessionServiceImpl.create(10L, request, organizer));
        verify(sessionRepository, never()).save(any());
    }

    // Mismo evento, mismo título y misma hora de inicio: sesión duplicada.
    @Test
    void create_deberiaLanzarExcepcion_cuandoSesionDuplicada() {
        User organizer = mock(User.class);
        when(organizer.getId()).thenReturn(1L);
        when(organizer.hasRole(RoleName.ADMIN)).thenReturn(false);
        Event event = eventWithId(10L, organizer);
        SessionRequest request = validSessionRequest();

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(sessionRepository.existsByEventIdAndTitleAndStartAt(10L, request.title(), request.startAt()))
                .thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> sessionServiceImpl.create(10L, request, organizer));
        verify(sessionRepository, never()).save(any());
    }

    // El dueño del evento puede actualizar una sesión que sí le pertenece.
    @Test
    void update_deberiaActualizarSesion_cuandoEsPropietario() {
        User organizer = mock(User.class);
        when(organizer.getId()).thenReturn(1L);
        when(organizer.hasRole(RoleName.ADMIN)).thenReturn(false);
        Event event = eventWithId(10L, organizer);
        Session session = new Session(event, "Charla vieja", Instant.now(), Instant.now().plusSeconds(3600));
        ReflectionTestUtils.setField(session, "id", 5L);

        SessionRequest request = validSessionRequest();
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(sessionRepository.findById(5L)).thenReturn(Optional.of(session));
        when(sessionMapper.toResponse(session)).thenReturn(sampleSessionResponse());

        SessionResponse response = sessionServiceImpl.update(10L, 5L, request, organizer);

        assertThat(response).isNotNull();
        assertThat(session.getTitle()).isEqualTo(request.title());
    }

    // Sesión que pertenece a otro evento: no debe encontrarse bajo este eventId.
    @Test
    void update_deberiaLanzarExcepcion_cuandoSesionNoPerteneceAlEvento() {
        User organizer = mock(User.class);
        when(organizer.getId()).thenReturn(1L);
        when(organizer.hasRole(RoleName.ADMIN)).thenReturn(false);
        Event event = eventWithId(10L, organizer);
        Event otherEvent = eventWithId(99L, organizer);
        Session session = new Session(otherEvent, "Charla", Instant.now(), Instant.now().plusSeconds(3600));
        ReflectionTestUtils.setField(session, "id", 5L);

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(sessionRepository.findById(5L)).thenReturn(Optional.of(session));

        SessionRequest request = validSessionRequest();
        assertThrows(ResourceNotFoundException.class, () -> sessionServiceImpl.update(10L, 5L, request, organizer));
    }

    // El dueño del evento puede eliminar una sesión propia.
    @Test
    void delete_deberiaEliminarSesion_cuandoEsPropietario() {
        User organizer = mock(User.class);
        when(organizer.getId()).thenReturn(1L);
        when(organizer.hasRole(RoleName.ADMIN)).thenReturn(false);
        Event event = eventWithId(10L, organizer);
        Session session = new Session(event, "Charla", Instant.now(), Instant.now().plusSeconds(3600));
        ReflectionTestUtils.setField(session, "id", 5L);

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(sessionRepository.findById(5L)).thenReturn(Optional.of(session));

        sessionServiceImpl.delete(10L, 5L, organizer);

        verify(sessionRepository).delete(session);
    }

    // Evento publicado: sus sesiones son visibles sin necesidad de autenticarse.
    @Test
    void listByEvent_deberiaListarSesiones_cuandoEventoPublico() {
        User organizer = mock(User.class);
        Event event = eventWithId(10L, organizer);
        event.setStatus(EventStatus.PUBLISHED);
        Session session = new Session(event, "Charla", Instant.now(), Instant.now().plusSeconds(3600));

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(sessionRepository.findByEventIdOrderByStartAtAsc(10L)).thenReturn(List.of(session));
        when(sessionMapper.toResponse(session)).thenReturn(sampleSessionResponse());

        List<SessionResponse> responses = sessionServiceImpl.listByEvent(10L, null);

        assertThat(responses).hasSize(1);
    }

    // Evento en borrador consultado por alguien que no es el dueño: se oculta como si no existiera.
    @Test
    void listByEvent_deberiaLanzarExcepcion_cuandoNoPublicadoYNoEsPropietario() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(1L);
        Event event = eventWithId(10L, owner);

        User other = mock(User.class);
        when(other.hasRole(RoleName.ADMIN)).thenReturn(false);
        when(other.getId()).thenReturn(2L);

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThrows(ResourceNotFoundException.class, () -> sessionServiceImpl.listByEvent(10L, other));
    }
}
