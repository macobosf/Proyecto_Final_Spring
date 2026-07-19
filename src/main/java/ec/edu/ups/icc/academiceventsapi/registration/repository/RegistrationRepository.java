package ec.edu.ups.icc.academiceventsapi.registration.repository;

import ec.edu.ups.icc.academiceventsapi.registration.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    Optional<Registration> findByRegistrationCode(UUID registrationCode);

    boolean existsByEventIdAndParticipantId(Long eventId, Long participantId);
}
