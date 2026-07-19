package ec.edu.ups.icc.academiceventsapi.registration.repository;

import ec.edu.ups.icc.academiceventsapi.registration.entity.Registration;
import ec.edu.ups.icc.academiceventsapi.registration.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, Long>,
        JpaSpecificationExecutor<Registration> {

    Optional<Registration> findByRegistrationCode(UUID registrationCode);

    boolean existsByEventIdAndParticipantId(Long eventId, Long participantId);

    boolean existsByEventId(Long eventId);

    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);
}
