package ec.edu.ups.icc.academiceventsapi.auth.exception;

import ec.edu.ups.icc.academiceventsapi.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Correo o contraseña inválidos.");
    }
}
