package ec.edu.ups.icc.academiceventsapi.registration.mapper;

import ec.edu.ups.icc.academiceventsapi.registration.dto.RegistrationResponse;
import ec.edu.ups.icc.academiceventsapi.registration.entity.Registration;
import org.springframework.stereotype.Component;

@Component
public class RegistrationMapper {

    public RegistrationResponse toResponse(Registration registration) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getRegistrationCode(),
                registration.getEvent().getId(),
                registration.getEvent().getTitle(),
                registration.getParticipant().getId(),
                registration.getParticipant().getFirstName() + " " + registration.getParticipant().getLastName(),
                registration.getStatus(),
                registration.getRegisteredAt(),
                registration.getStatusUpdatedAt(),
                registration.getConfirmedAt(),
                registration.getCancelledAt()
        );
    }
}
