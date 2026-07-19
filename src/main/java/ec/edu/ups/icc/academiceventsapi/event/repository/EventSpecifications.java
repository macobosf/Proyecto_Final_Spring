package ec.edu.ups.icc.academiceventsapi.event.repository;

import ec.edu.ups.icc.academiceventsapi.event.entity.Event;
import ec.edu.ups.icc.academiceventsapi.event.entity.EventModality;
import ec.edu.ups.icc.academiceventsapi.event.entity.EventStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<Event> hasStatus(EventStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Event> hasOrganizer(Long organizerId) {
        return (root, query, cb) -> organizerId == null ? null
                : cb.equal(root.get("organizer").get("id"), organizerId);
    }

    public static Specification<Event> hasCategory(Long categoryId) {
        return (root, query, cb) -> categoryId == null ? null
                : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Event> hasModality(EventModality modality) {
        return (root, query, cb) -> modality == null ? null : cb.equal(root.get("modality"), modality);
    }

    public static Specification<Event> titleContains(String search) {
        return (root, query, cb) -> (search == null || search.isBlank()) ? null
                : cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%");
    }

    public static Specification<Event> startAtFrom(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("startAt"), from);
    }

    public static Specification<Event> startAtTo(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("startAt"), to);
    }
}
