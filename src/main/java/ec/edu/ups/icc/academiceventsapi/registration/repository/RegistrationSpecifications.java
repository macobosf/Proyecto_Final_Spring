package ec.edu.ups.icc.academiceventsapi.registration.repository;

import ec.edu.ups.icc.academiceventsapi.registration.entity.Registration;
import ec.edu.ups.icc.academiceventsapi.registration.entity.RegistrationStatus;
import org.springframework.data.jpa.domain.Specification;

public final class RegistrationSpecifications {

    private RegistrationSpecifications() {
    }

    public static Specification<Registration> byParticipant(Long participantId) {
        return (root, query, cb) -> participantId == null ? null
                : cb.equal(root.get("participant").get("id"), participantId);
    }

    public static Specification<Registration> byEvent(Long eventId) {
        return (root, query, cb) -> eventId == null ? null
                : cb.equal(root.get("event").get("id"), eventId);
    }

    public static Specification<Registration> hasStatus(RegistrationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
}
