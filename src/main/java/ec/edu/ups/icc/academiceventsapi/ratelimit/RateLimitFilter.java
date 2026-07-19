package ec.edu.ups.icc.academiceventsapi.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ec.edu.ups.icc.academiceventsapi.auth.security.CustomUserDetails;
import ec.edu.ups.icc.academiceventsapi.common.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int PUBLIC_LIMIT = 60;
    private static final int AUTHENTICATED_LIMIT = 120;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public RateLimitFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/actuator/health".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String key;
        int limit;
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails principal) {
            key = "rate-limit:user:" + principal.getUser().getId();
            limit = AUTHENTICATED_LIMIT;
        } else {
            key = "rate-limit:ip:" + request.getRemoteAddr();
            limit = PUBLIC_LIMIT;
        }

        RateLimitResult result = rateLimiterService.tryConsume(key, limit, WINDOW);
        if (!result.allowed()) {
            writeTooManyRequests(response, request, result.retryAfterSeconds());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeTooManyRequests(HttpServletResponse response, HttpServletRequest request,
                                       long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiError error = new ApiError(Instant.now(), HttpStatus.TOO_MANY_REQUESTS.value(), "RATE_LIMIT_EXCEEDED",
                "Ha superado el límite de solicitudes. Intente nuevamente más tarde.",
                request.getRequestURI(), List.of());
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
