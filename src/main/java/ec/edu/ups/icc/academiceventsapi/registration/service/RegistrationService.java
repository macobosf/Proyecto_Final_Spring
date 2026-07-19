package ec.edu.ups.icc.academiceventsapi.registration.service;

import ec.edu.ups.icc.academiceventsapi.registration.dto.RegistrationResponse;
import ec.edu.ups.icc.academiceventsapi.registration.entity.RegistrationStatus;
import ec.edu.ups.icc.academiceventsapi.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RegistrationService {

    RegistrationResponse register(Long eventId, User participant);

    RegistrationResponse cancel(Long id, User participant);

    RegistrationResponse changeStatus(Long eventId, Long id, RegistrationStatus newStatus, User actor);

    RegistrationResponse getById(Long id, User actor);

    Page<RegistrationResponse> listMine(User participant, RegistrationStatus status, Pageable pageable);

    Page<RegistrationResponse> listByEvent(Long eventId, RegistrationStatus status, User actor, Pageable pageable);
}
