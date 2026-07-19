package ec.edu.ups.icc.academiceventsapi.auth.service;

import ec.edu.ups.icc.academiceventsapi.auth.dto.AuthResponse;
import ec.edu.ups.icc.academiceventsapi.auth.dto.LoginRequest;
import ec.edu.ups.icc.academiceventsapi.auth.dto.RefreshRequest;
import ec.edu.ups.icc.academiceventsapi.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request, String ip);

    AuthResponse refresh(RefreshRequest request, String ip);

    void logout(RefreshRequest request);
}
