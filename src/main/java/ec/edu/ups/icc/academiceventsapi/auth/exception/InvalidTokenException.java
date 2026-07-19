package ec.edu.ups.icc.academiceventsapi.auth.exception;

import ec.edu.ups.icc.academiceventsapi.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidTokenException extends ApiException {

    public InvalidTokenException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "El token proporcionado no es válido o ha expirado.");
    }
}
