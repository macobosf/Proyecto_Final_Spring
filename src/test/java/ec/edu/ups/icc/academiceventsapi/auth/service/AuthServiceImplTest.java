package ec.edu.ups.icc.academiceventsapi.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

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

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private RateLimiterService rateLimiterService;
    @Mock
    private LoginAttemptService loginAttemptService;

    private AuthServiceImpl authServiceImpl;

    // JwtProperties es un record (clase final): se usa una instancia real en vez de mockearla.
    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties("una-clave-secreta-de-prueba-bien-larga",
                Duration.ofMinutes(15), Duration.ofDays(7));
        authServiceImpl = new AuthServiceImpl(userRepository, roleRepository, userRoleRepository,
                refreshTokenRepository, passwordEncoder, authenticationManager, jwtService, jwtProperties,
                rateLimiterService, loginAttemptService);
    }

    // Correo nuevo, dentro del límite de registros: debe crear el usuario y emitir tokens.
    @Test
    void register_deberiaRegistrarUsuario_cuandoDatosValidos() {
        RegisterRequest request = new RegisterRequest("Ana", "Pérez", "ana.perez@example.com", "password123");
        when(rateLimiterService.tryConsume(eq("rate-limit:register:127.0.0.1"), eq(3), any(Duration.class)))
                .thenReturn(new RateLimitResult(true, 0));
        when(userRepository.existsByEmail("ana.perez@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByName(RoleName.PARTICIPANT))
                .thenReturn(Optional.of(new Role(RoleName.PARTICIPANT, "Participante")));
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.getAccessExpirationSeconds()).thenReturn(900L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authServiceImpl.register(request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(userRepository).save(any(User.class));
    }

    // No se permite registrar dos cuentas con el mismo correo.
    @Test
    void register_deberiaLanzarExcepcion_cuandoCorreoYaExiste() {
        RegisterRequest request = new RegisterRequest("Ana", "Pérez", "ana.perez@example.com", "password123");
        when(rateLimiterService.tryConsume(eq("rate-limit:register:127.0.0.1"), eq(3), any(Duration.class)))
                .thenReturn(new RateLimitResult(true, 0));
        when(userRepository.existsByEmail("ana.perez@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authServiceImpl.register(request, "127.0.0.1"));
        verify(userRepository, never()).save(any());
    }

    // Más de 3 registros por hora desde la misma IP: bloqueado por rate limiting.
    @Test
    void register_deberiaLanzarExcepcion_cuandoSuperaLimiteDeSolicitudes() {
        RegisterRequest request = new RegisterRequest("Ana", "Pérez", "ana.perez@example.com", "password123");
        when(rateLimiterService.tryConsume(eq("rate-limit:register:127.0.0.1"), eq(3), any(Duration.class)))
                .thenReturn(new RateLimitResult(false, 1800));

        assertThrows(RateLimitExceededException.class, () -> authServiceImpl.register(request, "127.0.0.1"));
        verify(userRepository, never()).existsByEmail(any());
    }

    // Credenciales correctas, sin bloqueo ni límite superado: debe autenticar y emitir tokens.
    @Test
    void login_deberiaAutenticar_cuandoCredencialesValidas() {
        LoginRequest request = new LoginRequest("ana.perez@example.com", "password123");
        String attemptKey = "127.0.0.1:ana.perez@example.com";
        User user = new User("Ana", "Pérez", "ana.perez@example.com", "hashed-password");

        when(loginAttemptService.isBlocked(attemptKey)).thenReturn(false);
        when(rateLimiterService.tryConsume(eq("rate-limit:login:" + attemptKey), eq(5), any(Duration.class)))
                .thenReturn(new RateLimitResult(true, 0));
        when(userRepository.findByEmail("ana.perez@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.getAccessExpirationSeconds()).thenReturn(900L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authServiceImpl.login(request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(loginAttemptService).registerSuccess(attemptKey);
    }

    // Cuenta bloqueada por intentos fallidos previos: ni siquiera intenta autenticar.
    @Test
    void login_deberiaLanzarExcepcion_cuandoCuentaBloqueada() {
        LoginRequest request = new LoginRequest("ana.perez@example.com", "password123");
        String attemptKey = "127.0.0.1:ana.perez@example.com";

        when(loginAttemptService.isBlocked(attemptKey)).thenReturn(true);
        when(loginAttemptService.getBlockRemainingSeconds(attemptKey)).thenReturn(600L);

        assertThrows(RateLimitExceededException.class, () -> authServiceImpl.login(request, "127.0.0.1"));
        verify(authenticationManager, never()).authenticate(any());
    }

    // Más de 5 intentos de login por minuto para el mismo IP+correo: bloqueado por rate limiting.
    @Test
    void login_deberiaLanzarExcepcion_cuandoSuperaLimiteDeSolicitudes() {
        LoginRequest request = new LoginRequest("ana.perez@example.com", "password123");
        String attemptKey = "127.0.0.1:ana.perez@example.com";

        when(loginAttemptService.isBlocked(attemptKey)).thenReturn(false);
        when(rateLimiterService.tryConsume(eq("rate-limit:login:" + attemptKey), eq(5), any(Duration.class)))
                .thenReturn(new RateLimitResult(false, 30));

        assertThrows(RateLimitExceededException.class, () -> authServiceImpl.login(request, "127.0.0.1"));
        verify(authenticationManager, never()).authenticate(any());
    }

    // Contraseña incorrecta: se registra el fallo (para el bloqueo temporal) y se rechaza el login.
    @Test
    void login_deberiaRegistrarFalloYLanzarExcepcion_cuandoCredencialesInvalidas() {
        LoginRequest request = new LoginRequest("ana.perez@example.com", "wrong-password");
        String attemptKey = "127.0.0.1:ana.perez@example.com";

        when(loginAttemptService.isBlocked(attemptKey)).thenReturn(false);
        when(rateLimiterService.tryConsume(eq("rate-limit:login:" + attemptKey), eq(5), any(Duration.class)))
                .thenReturn(new RateLimitResult(true, 0));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Credenciales inválidas"));

        assertThrows(InvalidCredentialsException.class, () -> authServiceImpl.login(request, "127.0.0.1"));
        verify(loginAttemptService).registerFailure(attemptKey);
    }

    // Refresh token válido y vigente: debe emitir un nuevo par de tokens y revocar el anterior.
    @Test
    void refresh_deberiaEmitirNuevosTokens_cuandoTokenValido() {
        RefreshRequest request = new RefreshRequest("raw-refresh-token");
        User user = new User("Ana", "Pérez", "ana.perez@example.com", "hashed-password");
        RefreshToken stored = new RefreshToken(user, "any-hash", Instant.now().plusSeconds(3600), "127.0.0.1");

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.getAccessExpirationSeconds()).thenReturn(900L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authServiceImpl.refresh(request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(stored.getRevokedAt()).isNotNull();
    }

    // Refresh token que no existe en la base: token inválido.
    @Test
    void refresh_deberiaLanzarExcepcion_cuandoTokenNoExiste() {
        RefreshRequest request = new RefreshRequest("raw-refresh-token");
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> authServiceImpl.refresh(request, "127.0.0.1"));
    }

    // Refresh token cuya fecha de expiración ya pasó.
    @Test
    void refresh_deberiaLanzarExcepcion_cuandoTokenExpirado() {
        RefreshRequest request = new RefreshRequest("raw-refresh-token");
        User user = new User("Ana", "Pérez", "ana.perez@example.com", "hashed-password");
        RefreshToken stored = new RefreshToken(user, "any-hash", Instant.now().minusSeconds(10), "127.0.0.1");

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        assertThrows(InvalidTokenException.class, () -> authServiceImpl.refresh(request, "127.0.0.1"));
    }

    // Refresh token ya revocado (por ejemplo, tras un logout previo).
    @Test
    void refresh_deberiaLanzarExcepcion_cuandoTokenRevocado() {
        RefreshRequest request = new RefreshRequest("raw-refresh-token");
        User user = new User("Ana", "Pérez", "ana.perez@example.com", "hashed-password");
        RefreshToken stored = new RefreshToken(user, "any-hash", Instant.now().plusSeconds(3600), "127.0.0.1");
        stored.setRevokedAt(Instant.now());

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        assertThrows(InvalidTokenException.class, () -> authServiceImpl.refresh(request, "127.0.0.1"));
    }

    // Logout con un refresh token existente: debe marcarse como revocado.
    @Test
    void logout_deberiaRevocarToken_cuandoExiste() {
        RefreshRequest request = new RefreshRequest("raw-refresh-token");
        User user = new User("Ana", "Pérez", "ana.perez@example.com", "hashed-password");
        RefreshToken stored = new RefreshToken(user, "any-hash", Instant.now().plusSeconds(3600), "127.0.0.1");

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        authServiceImpl.logout(request);

        assertThat(stored.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(stored);
    }

    // Logout con un token que ya no existe: no debe fallar, simplemente no hace nada.
    @Test
    void logout_noDeberiaFallar_cuandoTokenNoExiste() {
        RefreshRequest request = new RefreshRequest("raw-refresh-token");
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        authServiceImpl.logout(request);

        verify(refreshTokenRepository, never()).save(any());
    }
}
