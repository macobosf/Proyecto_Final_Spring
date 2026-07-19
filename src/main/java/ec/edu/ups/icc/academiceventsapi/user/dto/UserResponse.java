package ec.edu.ups.icc.academiceventsapi.user.dto;

import java.time.Instant;
import java.util.List;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String status,
        List<String> roles,
        Instant createdAt
) {
}
