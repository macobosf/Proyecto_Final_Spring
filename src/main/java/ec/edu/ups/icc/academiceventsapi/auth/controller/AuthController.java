package ec.edu.ups.icc.academiceventsapi.auth.controller;

import ec.edu.ups.icc.academiceventsapi.auth.dto.AuthResponse;
import ec.edu.ups.icc.academiceventsapi.auth.dto.LoginRequest;
import ec.edu.ups.icc.academiceventsapi.auth.dto.RefreshRequest;
import ec.edu.ups.icc.academiceventsapi.auth.dto.RegisterRequest;
import ec.edu.ups.icc.academiceventsapi.auth.security.CustomUserDetails;
import ec.edu.ups.icc.academiceventsapi.auth.service.AuthService;
import ec.edu.ups.icc.academiceventsapi.user.dto.UserResponse;
import ec.edu.ups.icc.academiceventsapi.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;

    public AuthController(AuthService authService, UserMapper userMapper) {
        this.authService = authService;
        this.userMapper = userMapper;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return authService.login(request, servletRequest.getRemoteAddr());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest servletRequest) {
        return authService.refresh(request, servletRequest.getRemoteAddr());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal CustomUserDetails principal) {
        return userMapper.toResponse(principal.getUser());
    }
}
