package ec.edu.ups.icc.academiceventsapi.category.dto;

import java.time.Instant;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
