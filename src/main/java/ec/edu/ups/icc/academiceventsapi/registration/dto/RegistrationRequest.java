package ec.edu.ups.icc.academiceventsapi.registration.dto;

import jakarta.validation.constraints.NotNull;

public record RegistrationRequest(
        @NotNull(message = "El evento es obligatorio.")
        Long eventId
) {
}
