package ec.edu.ups.icc.academiceventsapi.event.dto;

import ec.edu.ups.icc.academiceventsapi.event.entity.EventModality;
import ec.edu.ups.icc.academiceventsapi.event.entity.EventStatus;

import java.time.Instant;

public record EventResponse(
        Long id,
        String title,
        String description,
        EventModality modality,
        String location,
        String virtualUrl,
        Integer capacity,
        Integer availableCapacity,
        Instant registrationStartAt,
        Instant registrationEndAt,
        Instant startAt,
        Instant endAt,
        EventStatus status,
        Long organizerId,
        String organizerName,
        Long categoryId,
        String categoryName,
        Instant createdAt,
        Instant updatedAt
) {
}
