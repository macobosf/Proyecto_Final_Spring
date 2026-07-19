package ec.edu.ups.icc.academiceventsapi.auth.service;

import ec.edu.ups.icc.academiceventsapi.auth.dto.AuthResponse;
import ec.edu.ups.icc.academiceventsapi.auth.dto.LoginRequest;
import ec.edu.ups.icc.academiceventsapi.auth.dto.RefreshRequest;
import ec.edu.ups.icc.academiceventsapi.auth.dto.RegisterRequest;
import ec.edu.ups.icc.academiceventsapi.auth.entity.RefreshToken;
import ec.edu.ups.icc.academiceventsapi.auth.exception.InvalidCredentialsException;
import ec.edu.ups.icc.academiceventsapi.auth.exception.InvalidTokenException;
import ec.edu.ups.icc.academiceventsapi.auth.repository.RefreshTokenRepository;
import ec.edu.ups.icc.academiceventsapi.auth.security.JwtProperties;
import ec.edu.ups.icc.academiceventsapi.auth.security.JwtService;
import ec.edu.ups.icc.academiceventsapi.common.exception.DuplicateResourceException;
import ec.edu.ups.icc.academiceventsapi.ratelimit.LoginAttemptService;
import ec.edu.ups.icc.academiceventsapi.ratelimit.RateLimitExceededException;
import ec.edu.ups.icc.academiceventsapi.ratelimit.RateLimitResult;
import ec.edu.ups.icc.academiceventsapi.ratelimit.RateLimiterService;
import ec.edu.ups.icc.academiceventsapi.user.entity.Role;
import ec.edu.ups.icc.academiceventsapi.user.entity.RoleName;
import ec.edu.ups.icc.academiceventsapi.user.entity.User;
import ec.edu.ups.icc.academiceventsapi.user.entity.UserRole;
import ec.edu.ups.icc.academiceventsapi.user.repository.RoleRepository;
import ec.edu.ups.icc.academiceventsapi.user.repository.UserRepository;
import ec.edu.ups.icc.academiceventsapi.user.repository.UserRoleRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RateLimiterService rateLimiterService;
    private final LoginAttemptService loginAttemptService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                            UserRoleRepository userRoleRepository, RefreshTokenRepository refreshTokenRepository,
                            PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                            JwtService jwtService, JwtProperties jwtProperties,
                            RateLimiterService rateLimiterService, LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.rateLimiterService = rateLimiterService;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, String ip) {
        RateLimitResult rateLimit = rateLimiterService.tryConsume("rate-limit:register:" + ip, 3, Duration.ofHours(1));
        if (!rateLimit.allowed()) {
            throw new RateLimitExceededException("Demasiados registros desde esta dirección. Intente más tarde.",
                    rateLimit.retryAfterSeconds());
        }

        String email = request.email().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Ya existe una cuenta registrada con ese correo.");
        }

        User user = new User(request.firstName(), request.lastName(), email,
                passwordEncoder.encode(request.password()));
        User savedUser = userRepository.save(user);

        Role participantRole = roleRepository.findByName(RoleName.PARTICIPANT)
                .orElseThrow(() -> new IllegalStateException("El rol PARTICIPANT no existe en la base de datos."));
        UserRole userRole = userRoleRepository.save(new UserRole(savedUser, participantRole));
        savedUser.getUserRoles().add(userRole);

        return issueTokens(savedUser, null).response();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ip) {
        String email = request.email().toLowerCase();
        String attemptKey = ip + ":" + email;

        if (loginAttemptService.isBlocked(attemptKey)) {
            throw new RateLimitExceededException(
                    "Cuenta bloqueada temporalmente por demasiados intentos fallidos. Intente más tarde.",
                    loginAttemptService.getBlockRemainingSeconds(attemptKey));
        }

        RateLimitResult rateLimit = rateLimiterService.tryConsume("rate-limit:login:" + attemptKey, 5,
                Duration.ofMinutes(1));
        if (!rateLimit.allowed()) {
            throw new RateLimitExceededException(
                    "Demasiadas solicitudes de inicio de sesión. Intente más tarde.", rateLimit.retryAfterSeconds());
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException ex) {
            loginAttemptService.registerFailure(attemptKey);
            throw new InvalidCredentialsException();
        }

        loginAttemptService.registerSuccess(attemptKey);

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        return issueTokens(user, ip).response();
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshRequest request, String ip) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(request.refreshToken()))
                .orElseThrow(InvalidTokenException::new);

        if (stored.getRevokedAt() != null || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException();
        }

        IssuedTokens issued = issueTokens(stored.getUser(), ip);

        stored.setRevokedAt(Instant.now());
        stored.setReplacedByTokenId(issued.refreshToken().getTokenId());
        refreshTokenRepository.save(stored);

        return issued.response();
    }

    @Override
    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByTokenHash(hash(request.refreshToken()))
                .ifPresent(token -> {
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                });
    }

    private IssuedTokens issueTokens(User user, String ip) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = generateSecureToken();
        RefreshToken refreshToken = new RefreshToken(user, hash(rawRefreshToken),
                Instant.now().plus(jwtProperties.refreshExpiration()), ip);
        RefreshToken savedRefreshToken = refreshTokenRepository.save(refreshToken);

        AuthResponse response = new AuthResponse(accessToken, rawRefreshToken, "Bearer",
                jwtService.getAccessExpirationSeconds());
        return new IssuedTokens(response, savedRefreshToken);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private record IssuedTokens(AuthResponse response, RefreshToken refreshToken) {
    }
}
