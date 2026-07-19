package ec.edu.ups.icc.academiceventsapi.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds
) {
}
