package ec.edu.ups.icc.academiceventsapi.registration.dto;

import ec.edu.ups.icc.academiceventsapi.registration.entity.RegistrationStatus;
import jakarta.validation.constraints.NotNull;

public record RegistrationStatusRequest(
        @NotNull(message = "El estado es obligatorio.")
        RegistrationStatus status
) {
}
