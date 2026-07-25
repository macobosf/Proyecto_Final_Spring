package ec.edu.ups.icc.academiceventsapi.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

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
import ec.edu.ups.icc.academiceventsapi.event.entity.Event;
import ec.edu.ups.icc.academiceventsapi.event.entity.EventModality;
import ec.edu.ups.icc.academiceventsapi.event.entity.EventStatus;
import ec.edu.ups.icc.academiceventsapi.event.repository.EventRepository;
import ec.edu.ups.icc.academiceventsapi.registration.dto.RegistrationResponse;
import ec.edu.ups.icc.academiceventsapi.registration.entity.Registration;
import ec.edu.ups.icc.academiceventsapi.registration.entity.RegistrationStatus;
import ec.edu.ups.icc.academiceventsapi.registration.mapper.RegistrationMapper;
import ec.edu.ups.icc.academiceventsapi.registration.repository.RegistrationRepository;
import ec.edu.ups.icc.academiceventsapi.user.entity.RoleName;
import ec.edu.ups.icc.academiceventsapi.user.entity.User;

@ExtendWith(MockitoExtension.class)
public class RegistrationServiceImplTest {

    @InjectMocks
    private RegistrationServiceImpl registrationServiceImpl;
    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private RegistrationMapper registrationMapper;

    // Evento publicado, con el período de inscripción abierto ahora mismo.
    private Event publishedEvent(Long id, User organizer, int availableCapacity) {
        Instant now = Instant.now();
        Event event = new Event("Conferencia Java", "Descripción", EventModality.PRESENTIAL, 100,
                now.minusSeconds(3600), now.plusSeconds(3600), now.plusSeconds(7200), now.plusSeconds(10800),
                organizer, new Category("Tecnología", "Eventos de tecnología"));
        ReflectionTestUtils.setField(event, "id", id);
        event.setStatus(EventStatus.PUBLISHED);
        event.setAvailableCapacity(availableCapacity);
        return event;
    }

    private RegistrationResponse sampleRegistrationResponse() {
        Instant now = Instant.now();
        return new RegistrationResponse(1L, UUID.randomUUID(), 10L, "Conferencia Java", 1L, "Juan Pérez",
                RegistrationStatus.PENDING, now, now, null, null);
    }

    // Evento publicado, con cupo, período abierto y sin inscripción previa: debe registrar.
    @Test
    void register_deberiaCrearInscripcion_cuandoDatosValidos() {
        User participant = mock(User.class);
        when(participant.getId()).thenReturn(1L);
        Event event = publishedEvent(10L, mock(User.class), 5);

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByEventIdAndParticipantId(10L, 1L)).thenReturn(false);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(registrationMapper.toResponse(any(Registration.class))).thenReturn(sampleRegistrationResponse());

        RegistrationResponse response = registrationServiceImpl.register(10L, participant);

        assertThat(response).isNotNull();
        verify(registrationRepository).save(any(Registration.class));
    }

    // No se permite inscribirse en un evento que no está publicado.
    @Test
    void register_deberiaLanzarExcepcion_cuandoEventoNoPublicado() {
        User participant = mock(User.class);
        Event event = publishedEvent(10L, mock(User.class), 5);
        event.setStatus(EventStatus.DRAFT);

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThrows(BusinessRuleViolationException.class, () -> registrationServiceImpl.register(10L, participant));
        verify(registrationRepository, never()).save(any());
    }

    // El período de inscripción del evento ya cerró.
    @Test
    void register_deberiaLanzarExcepcion_cuandoFueraDePeriodoInscripcion() {
        User participant = mock(User.class);
        Instant now = Instant.now();
        Event event = new Event("Conferencia Java", "Descripción", EventModality.PRESENTIAL, 100,
                now.minusSeconds(7200), now.minusSeconds(3600), now.plusSeconds(7200), now.plusSeconds(10800),
                mock(User.class), new Category("Tecnología", "Eventos de tecnología"));
        ReflectionTestUtils.setField(event, "id", 10L);
        event.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThrows(BusinessRuleViolationException.class, () -> registrationServiceImpl.register(10L, participant));
        verify(registrationRepository, never()).save(any());
    }

    // Sin cupos disponibles, no se puede inscribir aunque el período esté abierto.
    @Test
    void register_deberiaLanzarExcepcion_cuandoSinCupos() {
        User participant = mock(User.class);
        Event event = publishedEvent(10L, mock(User.class), 0);

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThrows(BusinessRuleViolationException.class, () -> registrationServiceImpl.register(10L, participant));
        verify(registrationRepository, never()).save(any());
    }

    // El mismo participante no puede inscribirse dos veces en el mismo evento.
    @Test
    void register_deberiaLanzarExcepcion_cuandoYaInscrito() {
        User participant = mock(User.class);
        when(participant.getId()).thenReturn(1L);
        Event event = publishedEvent(10L, mock(User.class), 5);

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByEventIdAndParticipantId(10L, 1L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> registrationServiceImpl.register(10L, participant));
        verify(registrationRepository, never()).save(any());
    }

    // El propio participante puede cancelar su inscripción pendiente.
    @Test
    void cancel_deberiaCancelarInscripcion_cuandoEsPropietario() {
        User participant = mock(User.class);
        when(participant.getId()).thenReturn(1L);
        Event event = publishedEvent(10L, mock(User.class), 5);
        Registration registration = new Registration(event, participant);

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(registrationMapper.toResponse(registration)).thenReturn(sampleRegistrationResponse());

        RegistrationResponse response = registrationServiceImpl.cancel(1L, participant);

        assertThat(response).isNotNull();
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
        assertThat(event.getAvailableCapacity()).isEqualTo(5);
    }

    // Si la inscripción cancelada estaba confirmada, el cupo del evento debe restaurarse.
    @Test
    void cancel_deberiaRestaurarCupo_cuandoEstabaConfirmada() {
        User participant = mock(User.class);
        when(participant.getId()).thenReturn(1L);
        Event event = publishedEvent(10L, mock(User.class), 5);
        Registration registration = new Registration(event, participant);
        registration.setStatus(RegistrationStatus.CONFIRMED);

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(registrationMapper.toResponse(registration)).thenReturn(sampleRegistrationResponse());

        registrationServiceImpl.cancel(1L, participant);

        assertThat(event.getAvailableCapacity()).isEqualTo(6);
    }

    // Un participante no puede cancelar la inscripción de otro.
    @Test
    void cancel_deberiaLanzarExcepcion_cuandoNoEsPropietario() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(1L);
        User other = mock(User.class);
        when(other.getId()).thenReturn(2L);

        Event event = publishedEvent(10L, mock(User.class), 5);
        Registration registration = new Registration(event, owner);

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));

        assertThrows(AccessDeniedException.class, () -> registrationServiceImpl.cancel(1L, other));
    }

    // Una inscripción ya cancelada no se puede volver a cancelar.
    @Test
    void cancel_deberiaLanzarExcepcion_cuandoYaCancelada() {
        User participant = mock(User.class);
        when(participant.getId()).thenReturn(1L);
        Event event = publishedEvent(10L, mock(User.class), 5);
        Registration registration = new Registration(event, participant);
        registration.setStatus(RegistrationStatus.CANCELLED);

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));

        assertThrows(BusinessRuleViolationException.class, () -> registrationServiceImpl.cancel(1L, participant));
    }

    // El organizador confirma una inscripción pendiente y hay cupo: se descuenta el cupo.
    @Test
    void changeStatus_deberiaConfirmarInscripcion_cuandoHayCupo() {
        User organizer = mock(User.class);
        when(organizer.getId()).thenReturn(1L);
        when(organizer.hasRole(RoleName.ADMIN)).thenReturn(false);
        Event event = publishedEvent(10L, organizer, 5);
        Registration registration = new Registration(event, mock(User.class));

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(registrationMapper.toResponse(registration)).thenReturn(sampleRegistrationResponse());

        RegistrationResponse response =
                registrationServiceImpl.changeStatus(10L, 1L, RegistrationStatus.CONFIRMED, organizer);

        assertThat(response).isNotNull();
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
        assertThat(event.getAvailableCapacity()).isEqualTo(4);
    }

    // Sin cupo disponible no se puede confirmar, aunque la inscripción esté pendiente.
    @Test
    void changeStatus_deberiaLanzarExcepcion_cuandoSinCupo() {
        User organizer = mock(User.class);
        when(organizer.getId()).thenReturn(1L);
        when(organizer.hasRole(RoleName.ADMIN)).thenReturn(false);
        Event event = publishedEvent(10L, organizer, 0);
        Registration registration = new Registration(event, mock(User.class));

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));

        assertThrows(BusinessRuleViolationException.class,
                () -> registrationServiceImpl.changeStatus(10L, 1L, RegistrationStatus.CONFIRMED, organizer));
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.PENDING);
    }

    // Este endpoint solo admite confirmar o rechazar, no cualquier estado.
    @Test
    void changeStatus_deberiaLanzarExcepcion_cuandoEstadoSolicitadoNoValido() {
        User organizer = mock(User.class);

        assertThrows(BusinessRuleViolationException.class,
                () -> registrationServiceImpl.changeStatus(10L, 1L, RegistrationStatus.CANCELLED, organizer));
        verify(eventRepository, never()).findById(any());
    }

    // Solo se pueden confirmar/rechazar inscripciones que sigan en estado PENDING.
    @Test
    void changeStatus_deberiaLanzarExcepcion_cuandoNoEstaPending() {
        User organizer = mock(User.class);
        when(organizer.getId()).thenReturn(1L);
        when(organizer.hasRole(RoleName.ADMIN)).thenReturn(false);
        Event event = publishedEvent(10L, organizer, 5);
        Registration registration = new Registration(event, mock(User.class));
        registration.setStatus(RegistrationStatus.CONFIRMED);

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));

        assertThrows(BusinessRuleViolationException.class,
                () -> registrationServiceImpl.changeStatus(10L, 1L, RegistrationStatus.REJECTED, organizer));
    }

    // El propio participante puede ver el detalle de su inscripción.
    @Test
    void getById_deberiaRetornarInscripcion_cuandoEsParticipante() {
        User participant = mock(User.class);
        when(participant.getId()).thenReturn(1L);
        User organizer = mock(User.class);
        when(organizer.getId()).thenReturn(2L);
        Event event = publishedEvent(10L, organizer, 5);
        Registration registration = new Registration(event, participant);

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(registrationMapper.toResponse(registration)).thenReturn(sampleRegistrationResponse());

        RegistrationResponse response = registrationServiceImpl.getById(1L, participant);

        assertThat(response).isNotNull();
    }

    // Un tercero sin relación con la inscripción (ni ADMIN) no puede verla.
    @Test
    void getById_deberiaLanzarExcepcion_cuandoNoTienePermiso() {
        User participant = mock(User.class);
        when(participant.getId()).thenReturn(1L);
        User organizer = mock(User.class);
        when(organizer.getId()).thenReturn(2L);
        Event event = publishedEvent(10L, organizer, 5);
        Registration registration = new Registration(event, participant);

        User stranger = mock(User.class);
        when(stranger.getId()).thenReturn(3L);
        when(stranger.hasRole(RoleName.ADMIN)).thenReturn(false);

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));

        assertThrows(AccessDeniedException.class, () -> registrationServiceImpl.getById(1L, stranger));
    }
}
