package ec.edu.ups.icc.academiceventsapi.ratelimit;

public record RateLimitResult(boolean allowed, long retryAfterSeconds) {
}
