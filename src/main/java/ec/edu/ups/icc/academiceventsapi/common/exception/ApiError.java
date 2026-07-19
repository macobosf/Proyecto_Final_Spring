package ec.edu.ups.icc.academiceventsapi.common.exception;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path,
        List<FieldValidationError> fieldErrors
) {
    public ApiError {
        if (fieldErrors == null) {
            fieldErrors = List.of();
        }
    }
}
