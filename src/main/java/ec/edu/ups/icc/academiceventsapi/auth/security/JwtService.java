package ec.edu.ups.icc.academiceventsapi.auth.security;

import ec.edu.ups.icc.academiceventsapi.user.entity.User;
import ec.edu.ups.icc.academiceventsapi.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class JwtService {

    private final SecretKey signingKey;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.accessExpiration());
        List<String> roles = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .map(role -> role.getName().name())
                .toList();

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public long getAccessExpirationSeconds() {
        return properties.accessExpiration().toSeconds();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
