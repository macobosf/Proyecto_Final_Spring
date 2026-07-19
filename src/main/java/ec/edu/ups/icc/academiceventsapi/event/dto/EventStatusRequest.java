package ec.edu.ups.icc.academiceventsapi.event.dto;

import ec.edu.ups.icc.academiceventsapi.event.entity.EventStatus;
import jakarta.validation.constraints.NotNull;

public record EventStatusRequest(
        @NotNull(message = "El estado es obligatorio.")
        EventStatus status
) {
}
