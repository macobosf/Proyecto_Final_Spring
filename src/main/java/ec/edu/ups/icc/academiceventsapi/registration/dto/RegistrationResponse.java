package ec.edu.ups.icc.academiceventsapi.registration.dto;

import ec.edu.ups.icc.academiceventsapi.registration.entity.RegistrationStatus;

import java.time.Instant;
import java.util.UUID;

public record RegistrationResponse(
        Long id,
        UUID registrationCode,
        Long eventId,
        String eventTitle,
        Long participantId,
        String participantName,
        RegistrationStatus status,
        Instant registeredAt,
        Instant statusUpdatedAt,
        Instant confirmedAt,
        Instant cancelledAt
) {
}
